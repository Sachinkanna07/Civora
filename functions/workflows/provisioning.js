const {onCall,HttpsError,db,serverTime,requireAuth,text,id,fail,record,Timestamp,getAuth,randomBytes,foodTransitions,reportTransitions,canTransition,hasCapacity,priceOrder} = require('../shared');
exports.invitePrivilegedUser = onCall('invitePrivilegedUser',async req => {
  const {uid} = requireAuth(req, ['admin']); const email = text(req.data.email,254); const role = text(req.data.role,30);
  if (!['faculty','vendor','driver','maintenance','mess'].includes(role) || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) fail('Invalid invitation.');
  const name = text(req.data.name,100); const department = text(req.data.department || '',80,false);
  const team = text(req.data.team||'Campus operations',80);
  let user; try { user = await getAuth().getUserByEmail(email); } catch (e) { if (e.code !== 'auth/user-not-found') throw e; user = await getAuth().createUser({email,displayName:name}); }
  const previousRole = user.customClaims?.role;
  if (previousRole === 'admin') throw new HttpsError('permission-denied','Administrator accounts cannot be modified through invitations.');
  await db.doc(`users/${user.uid}`).set({uid:user.uid,name,email,role,department,year:'',section:'',routeId:'',profileImage:'',notificationsEnabled:true,createdAt:serverTime()}, {merge:true});
  await getAuth().setCustomUserClaims(user.uid, {...user.customClaims,role});
  await db.doc(`staffDirectory/${user.uid}`).set({uid:user.uid,name,role,department,team,active:true},{merge:true});
  const resetLink = await getAuth().generatePasswordResetLink(email);
  await db.collection('roleAudit').add({actor:uid,target:user.uid,role,createdAt:serverTime()});
  return {uid:user.uid,setupLink:resetLink};
});

const responsibilityQueries=async uid=>{
  const [reports,orders,buses,trips,events,canteens,registrations,messHalls]=await Promise.all([
    db.collection('campusReports').where('assignedTo','==',uid).where('status','in',['ASSIGNED','ACCEPTED','IN_PROGRESS','REVIEW_REQUIRED']).limit(101).get(),
    db.collection('foodOrders').where('vendorId','==',uid).where('status','in',['PLACED','ACCEPTED','PREPARING','READY']).limit(101).get(),
    db.collection('buses').where('driverId','==',uid).limit(101).get(),
    db.collection('activeTrips').where('driverId','==',uid).where('active','==',true).limit(101).get(),
    db.collection('events').where('createdBy','==',uid).limit(101).get(),
    db.collection('canteens').where('vendorId','==',uid).limit(101).get(),
    db.collection('eventRegistrations').where('organizerId','==',uid).limit(101).get(),
    db.collection('messHalls').where('operatorId','==',uid).limit(101).get(),
  ]);
  const sets=[reports,orders,buses,trips,events,canteens,registrations,messHalls];
  if(sets.some(x=>x.size>100)||sets.reduce((n,x)=>n+x.size,0)>350)fail('This account has too many responsibilities for an interactive transfer. Use the reviewed bulk migration procedure.');
  return {reports,orders,buses,trips,events,canteens,registrations,messHalls};
};

exports.previewStaffDeactivation=onCall('previewStaffDeactivation',async req=>{
  const {uid:actor}=requireAuth(req,['admin']),targetUid=id(req.data.targetUid);
  if(targetUid===actor)fail('Another administrator must manage your account.');
  const [target,directory,responsibilities]=await Promise.all([getAuth().getUser(targetUid),db.doc(`staffDirectory/${targetUid}`).get(),responsibilityQueries(targetUid)]);
  const role=target.customClaims?.role;
  if(!['faculty','vendor','driver','maintenance','mess','admin'].includes(role)||!directory.exists||directory.data().active!==true)fail('Select an active staff account.');
  return {targetUid,name:directory.data().name||target.displayName||target.email,role,reports:responsibilities.reports.size,orders:responsibilities.orders.size,buses:responsibilities.buses.size,trips:responsibilities.trips.size,events:responsibilities.events.size,canteens:responsibilities.canteens.size,registrations:responsibilities.registrations.size,messHalls:responsibilities.messHalls.size};
});

exports.deactivateStaffAccount=onCall('deactivateStaffAccount',async req=>{
  const {uid:actor}=requireAuth(req,['admin']),targetUid=id(req.data.targetUid),replacementUid=id(req.data.replacementUid);
  if(req.data.confirm!==true)throw new HttpsError('invalid-argument','Confirm the responsibility transfer and account deactivation.');
  if(targetUid===actor||targetUid===replacementUid)fail('Choose a different active replacement.');
  const [target,replacement,targetDirectory,replacementDirectory,responsibilities]=await Promise.all([
    getAuth().getUser(targetUid),getAuth().getUser(replacementUid),db.doc(`staffDirectory/${targetUid}`).get(),db.doc(`staffDirectory/${replacementUid}`).get(),responsibilityQueries(targetUid),
  ]);
  const role=target.customClaims?.role,replacementRole=replacement.customClaims?.role;
  if(!['faculty','vendor','driver','maintenance','mess','admin'].includes(role)||!targetDirectory.exists)fail('Select a staff account.');
  if(replacement.disabled||replacementRole!==role||!replacementDirectory.exists||replacementDirectory.data().active!==true)fail(`Choose an active ${role} replacement.`);
  const priorTransfer=targetDirectory.data().active===false&&targetDirectory.data().replacementUid===replacementUid;
  if(targetDirectory.data().active===false&&!priorTransfer)fail('This account is already inactive or was transferred to a different replacement.');
  const replacementName=replacementDirectory.data().name||replacement.displayName||replacement.email;
  if(!priorTransfer)await db.runTransaction(async tx=>{
    const targetProfile=await tx.get(db.doc(`users/${targetUid}`));
    if(!targetProfile.exists||targetProfile.data().accessDisabled===true)fail('This account is already inactive.');
    tx.update(targetProfile.ref,{accessDisabled:true,deactivatedAt:serverTime(),deactivatedBy:actor});
    for(const report of responsibilities.reports.docs)tx.update(report.ref,{assignedTo:replacementUid,assignedToName:replacementName,updatedAt:serverTime()});
    for(const order of responsibilities.orders.docs)tx.update(order.ref,{vendorId:replacementUid,updatedAt:serverTime()});
    for(const canteen of responsibilities.canteens.docs)tx.update(canteen.ref,{vendorId:replacementUid});
    for(const bus of responsibilities.buses.docs)tx.update(bus.ref,{driverId:replacementUid});
    for(const trip of responsibilities.trips.docs)tx.update(trip.ref,{active:false,endedAt:serverTime(),updatedAt:serverTime(),endReason:'DRIVER_TRANSFER'});
    for(const event of responsibilities.events.docs)tx.update(event.ref,{createdBy:replacementUid,organizer:replacementName});
    for(const registration of responsibilities.registrations.docs)tx.update(registration.ref,{organizerId:replacementUid});
    for(const hall of responsibilities.messHalls.docs)tx.update(hall.ref,{operatorId:replacementUid});
    tx.update(db.doc(`staffDirectory/${targetUid}`),{active:false,deactivatedAt:serverTime(),deactivatedBy:actor,replacementUid});
    tx.set(db.collection('roleAudit').doc(),{actor,target:targetUid,replacement:replacementUid,role,action:'STAFF_DEACTIVATED_AND_TRANSFERRED',counts:{reports:responsibilities.reports.size,orders:responsibilities.orders.size,buses:responsibilities.buses.size,trips:responsibilities.trips.size,events:responsibilities.events.size,canteens:responsibilities.canteens.size,registrations:responsibilities.registrations.size,messHalls:responsibilities.messHalls.size},createdAt:serverTime()});
  });
  try {await getAuth().updateUser(targetUid,{disabled:true});await getAuth().revokeRefreshTokens(targetUid);}
  catch(error){require('firebase-functions/logger').error('Staff access was blocked in Firestore but Auth disable requires retry',{targetUid,code:error.code||'unknown'});throw new HttpsError('internal','Campus access was blocked, but Authentication cleanup requires an administrator retry.');}
  return {deactivated:true,targetUid,replacementUid,resumed:priorTransfer};
});

exports.deleteDeactivatedStaffAccount=onCall('deleteDeactivatedStaffAccount',async req=>{
  const {uid:actor}=requireAuth(req,['admin']),targetUid=id(req.data.targetUid);
  if(req.data.confirmPhrase!=='DELETE')throw new HttpsError('invalid-argument','Type DELETE to confirm permanent account deletion.');
  if(targetUid===actor)fail('Another administrator must manage your account.');
  if(!require('../deletion-policy').deletionEnabled())throw new HttpsError('failed-precondition','Production deletion is awaiting reviewed retention approval.');
  const [directory,profile,responsibilities]=await Promise.all([
    db.doc(`staffDirectory/${targetUid}`).get(),db.doc(`users/${targetUid}`).get(),responsibilityQueries(targetUid),
  ]);
  if(!directory.exists||directory.data().active!==false||!directory.data().replacementUid)fail('Transfer responsibilities and deactivate this account first.');
  const remaining=Object.values(responsibilities).reduce((count,set)=>count+set.size,0);
  if(remaining)fail('This account still owns active responsibilities. Review the transfer before deletion.');
  if(profile.exists&&profile.data().accessDisabled!==true)fail('Disable campus access before deletion.');
  const receipt=db.doc(`staffDeletions/${targetUid}`);
  await receipt.set({status:'PROCESSING',targetUid,requestedBy:actor,updatedAt:serverTime()},{merge:true});
  try {
    if(profile.exists)await db.recursiveDelete(profile.ref);
    await getAuth().deleteUser(targetUid).catch(error=>{if(error.code!=='auth/user-not-found')throw error;});
    await db.runTransaction(async tx=>{
      tx.set(directory.ref,{uid:targetUid,role:directory.data().role||'',team:directory.data().team||'',active:false,replacementUid:directory.data().replacementUid,deletionStatus:'COMPLETED',deletedAt:serverTime(),deletedBy:actor});
      tx.set(receipt,{status:'COMPLETED',targetUid,completedAt:serverTime()},{merge:true});
      tx.set(db.doc(`roleAudit/staff_delete_${targetUid}`),{actor,target:targetUid,replacement:directory.data().replacementUid,role:directory.data().role||'',action:'STAFF_ACCOUNT_DELETED',createdAt:serverTime()});
    });
  } catch(error) {
    await receipt.set({status:'RETRY_REQUIRED',targetUid,updatedAt:serverTime()},{merge:true});
    require('firebase-functions/logger').error('Staff account deletion requires retry',{targetUid,code:error.code||'unknown'});
    throw new HttpsError('internal','Staff account deletion requires an administrator retry.');
  }
  return {deleted:true,targetUid};
});
