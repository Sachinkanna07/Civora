const {onCall,HttpsError,db,requireAuth,serverTime,getAuth} = require('../shared');
const {onDocumentCreated} = require('firebase-functions/v2/firestore');
const {getStorage} = require('firebase-admin/storage');

exports.requestAccountDeletion = onCall('requestAccountDeletion',async req => {
  const {uid,role} = requireAuth(req);
  if(!require('../deletion-policy').deletionEnabled())throw new HttpsError('failed-precondition','Production deletion is awaiting reviewed retention approval. Use the configured Civora support channel.');
  if (req.data.confirm !== true) throw new HttpsError('invalid-argument','Confirm account deletion.');
  if (!Number.isFinite(req.auth.token.auth_time) || Date.now()/1000-req.auth.token.auth_time > 300)
    throw new HttpsError('unauthenticated','Confirm your password before deleting your account.');
  // Staff must hand over institutional resources; self-service cannot destroy
  // another user's kitchen, ticket, trip or maintenance assignment.
  if (role !== 'student') throw new HttpsError('failed-precondition','Contact your campus administrator to transfer staff responsibilities and delete this account.');
  await db.runTransaction(async tx=>{
    const activeOrders = await tx.get(db.collection('foodOrders').where('userId','==',uid).where('status','in',['PLACED','ACCEPTED','PREPARING','READY']).limit(1));
    if (!activeOrders.empty) throw new HttpsError('failed-precondition','Complete or cancel your active food order before deleting your account.');
    tx.create(db.doc(`accountDeletions/${uid}`),{status:'PENDING',requestedAt:serverTime()});
    tx.set(db.doc(`users/${uid}`),{accessDisabled:true},{merge:true});
  });
  return {accepted:true};
},{requiresAdmission:false});

async function removeOwned(collection,field,uid,remove) {
  for (;;) {
    const page = await db.collection(collection).where(field,'==',uid).limit(100).get();
    if (page.empty) return;
    for (const doc of page.docs) await (remove ? remove(doc) : db.recursiveDelete(doc.ref));
  }
}

// A durable request survives app closure and retries after transient failures.
// Authentication is removed last; repeated cleanup is idempotent.
async function deleteStudentData(uid) {
  await getAuth().updateUser(uid,{disabled:true}).catch(e=>{if(e.code!=='auth/user-not-found')throw e;});
  await removeOwned('eventRegistrations','userId',uid,async reg=>{
    await db.runTransaction(async tx=>{
      const current=await tx.get(reg.ref);if(!current.exists)return;
      const event=await tx.get(db.doc(`events/${current.data().eventId}`));
      if(event.exists)tx.update(event.ref,{registeredCount:Math.max(0,(event.data().registeredCount||0)-1)});
      tx.delete(reg.ref);
    });
  });
  await removeOwned('campusReports','createdBy',uid,async report=>{
    await getStorage().bucket().deleteFiles({prefix:`reportEvidence/${report.id}/`});
    await db.recursiveDelete(report.ref);
  });
  for(const collection of ['activities','notifications','clubFollowers','eventAttendance','foodOrders','mealEntitlements','mealCheckIns'])
    await removeOwned(collection,'userId',uid);
  await db.doc(`mealPasses/${uid}`).delete();
  await getStorage().bucket().deleteFiles({prefix:`reports/${uid}/`});
  await db.recursiveDelete(db.doc(`users/${uid}`));
  await getAuth().deleteUser(uid).catch(e=>{if(e.code!=='auth/user-not-found')throw e;});
}
exports.completeAccountDeletion = onDocumentCreated({document:'accountDeletions/{uid}',retry:true,timeoutSeconds:540,maxInstances:2},async event=>{
  if(!event.data)return;
  const ref=event.data.ref;
  if((await ref.get()).data()?.status==='COMPLETED')return;
  if(!require('../deletion-policy').deletionEnabled()) {
    await ref.set({status:'APPROVAL_REQUIRED',updatedAt:serverTime()},{merge:true});
    return;
  }
  await ref.set({status:'PROCESSING',updatedAt:serverTime()},{merge:true});
  try {
    await deleteStudentData(event.params.uid);
    // Keep only a restricted deletion receipt. No email, name, tokens or media.
    await ref.set({status:'COMPLETED',completedAt:serverTime()},{merge:true});
  } catch(error) {
    await ref.set({status:'RETRY_REQUIRED',updatedAt:serverTime()},{merge:true});
    require('firebase-functions/logger').error('Account deletion requires retry',{code:error.code||'unknown'});
    throw error;
  }
});
