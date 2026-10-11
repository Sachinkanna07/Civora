const {onCall,HttpsError,db,requireAuth,requireActiveAccount,getAuth,id,text,serverTime}=require('../shared');
const {eligible,canonicalEmail}=require('../admission-policy');
exports.lookupAdmissionCandidate=onCall('lookupAdmissionCandidate',async req=>{
  requireAuth(req,['admin']);
  const email=canonicalEmail(text(req.data.email,254));
  if(!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email))throw new HttpsError('invalid-argument','Enter a valid account email.');
  const user=await getAuth().getUserByEmail(email).catch(error=>{
    if(error.code==='auth/user-not-found')throw new HttpsError('not-found','No registered account found for that email.');
    throw new HttpsError('unavailable','Account lookup is unavailable. Check your connection and retry.');
  });
  const profile=await db.doc(`users/${user.uid}`).get();
  if(!profile.exists)throw new HttpsError('not-found','This account has no Civora profile.');
  return {uid:user.uid,name:profile.data().name,email:user.email,role:user.customClaims?.role||'student',emailVerified:user.emailVerified,exception:profile.data().admissionException===true,admitted:profile.data().membershipGranted===true,disabled:user.disabled||profile.data().accessDisabled===true};
});
exports.activateCampusAccess=onCall('activateCampusAccess',async req=>{
  const {uid,role}=requireAuth(req),user=await getAuth().getUser(uid);
  await db.runTransaction(async tx=>{
    await requireActiveAccount(tx,uid);
    const ref=db.doc(`users/${uid}`),profile=await tx.get(ref),p=profile.data();
    if(!profile.exists)throw new HttpsError('failed-precondition','Complete your Civora profile first.');
    if(p.accessDisabled===true||p.role!==role)throw new HttpsError('permission-denied','Your access changed. Sign in again.');
    if(!eligible(user,req.auth.token,p))throw new HttpsError('permission-denied','A verified institutional email or approved email exception is required.');
    if(p.membershipGranted!==true||p.membershipEmail!==canonicalEmail(user.email))
      tx.update(ref,{membershipGranted:true,membershipEmail:canonicalEmail(user.email),membershipGrantedAt:serverTime()});
  });
  return {admitted:true};
},{requiresAdmission:false});
exports.setAdmissionException=onCall('setAdmissionException',async req=>{
  const {uid:actor}=requireAuth(req,['admin']),uid=id(req.data.userId);
  if(typeof req.data.approve!=='boolean')throw new HttpsError('invalid-argument','Choose approve or revoke.');
  const approve=req.data.approve,reference=text(req.data.approvalReference,300);
  if(uid===actor&&!approve)throw new HttpsError('failed-precondition','Another administrator must revoke your own exception.');
  const user=await getAuth().getUser(uid),ref=db.doc(`users/${uid}`);
  await db.runTransaction(async tx=>{
    await requireActiveAccount(tx,uid);
    if(!(await tx.get(ref)).exists)throw new HttpsError('failed-precondition','The user must create a Civora profile first.');
    tx.update(ref,{admissionException:approve,admissionEmail:approve?canonicalEmail(user.email):'',membershipGranted:false,membershipEmail:''});
    tx.set(db.collection('roleAudit').doc(),{actor,target:uid,action:approve?'ADMISSION_EXCEPTION_APPROVED':'ADMISSION_EXCEPTION_REVOKED',approvalReference:reference,createdAt:serverTime()});
  });
  return {updated:true};
});
