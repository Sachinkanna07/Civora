require('./local-environment');
const {test}=require('node:test');
const assert=require('node:assert/strict');
const enabled=!!process.env.FIREBASE_AUTH_EMULATOR_HOST&&!!process.env.FIRESTORE_EMULATOR_HOST;

test('staff deactivation transfers every supported responsibility and revokes access',{skip:!enabled,timeout:180000},async()=>{
  const {initializeApp}=require('firebase-admin/app');const {getAuth}=require('firebase-admin/auth');const {getFirestore,Timestamp}=require('firebase-admin/firestore');
  const projectId=process.env.GCLOUD_PROJECT,app=initializeApp({projectId},`staff-transfer-${Date.now()}`),auth=getAuth(app),db=getFirestore(app),suffix=Date.now();
  async function actor(role,label){const uid=`${label}_${suffix}`,email=`${uid}@kpriet.ac.in`,password='EmulatorOnly123!';await auth.createUser({uid,email,password,emailVerified:true});await auth.setCustomUserClaims(uid,{role});await db.doc(`users/${uid}`).set({uid,email,name:label,role,department:'CSE',year:'',section:'',routeId:'',profileImage:'',notificationsEnabled:false,membershipGranted:true,membershipEmail:email,createdAt:Timestamp.now()});if(role!=='student')await db.doc(`staffDirectory/${uid}`).set({uid,name:label,role,department:'CSE',team:'Test',active:true});const response=await fetch(`http://${process.env.FIREBASE_AUTH_EMULATOR_HOST}/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=demo-key`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email,password,returnSecureToken:true})});return {uid,token:(await response.json()).idToken};}
  async function call(user,name,data){const host=process.env.CIVORA_FUNCTIONS_EMULATOR_HOST||'127.0.0.1:5001';return (await fetch(`http://${host}/${projectId}/us-central1/${name}`,{method:'POST',headers:{'Content-Type':'application/json',Authorization:`Bearer ${user.token}`},body:JSON.stringify({data})})).json();}
  const admin=await actor('admin','transfer_admin');
  for(const role of ['maintenance','vendor','driver','faculty','mess','admin']){
    const target=await actor(role,`${role}_old`),replacement=await actor(role,`${role}_new`),id=`${role}_${suffix}`;
    if(role==='maintenance')await db.doc(`campusReports/${id}`).set({createdBy:admin.uid,assignedTo:target.uid,assignedToName:'old',status:'ASSIGNED',createdAt:Timestamp.now()});
    if(role==='vendor'){await db.doc(`canteens/${id}`).set({name:'Kitchen',vendorId:target.uid,open:true});await db.doc(`foodOrders/${id}`).set({userId:admin.uid,vendorId:target.uid,status:'PLACED',createdAt:Timestamp.now()});}
    if(role==='driver'){await db.doc(`buses/${id}`).set({name:'Bus',driverId:target.uid,routeId:'route'});await db.doc(`activeTrips/${id}`).set({busId:id,driverId:target.uid,routeId:'route',active:true,startedAt:Timestamp.now(),updatedAt:Timestamp.now()});}
    if(role==='faculty'){await db.doc(`events/${id}`).set({createdBy:target.uid,organizer:'old',title:'Event'});await db.doc(`eventRegistrations/${id}_student`).set({eventId:id,userId:'student',organizerId:target.uid});}
    if(role==='mess')await db.doc(`messHalls/${id}`).set({name:'Hall',operatorId:target.uid,open:true});
    const preview=await call(admin,'previewStaffDeactivation',{targetUid:target.uid});assert.equal(preview.result?.role,role,JSON.stringify(preview));
    const result=await call(admin,'deactivateStaffAccount',{targetUid:target.uid,replacementUid:replacement.uid,confirm:true});assert.equal(result.result?.deactivated,true,JSON.stringify(result));
    assert.equal((await auth.getUser(target.uid)).disabled,true);assert.equal((await db.doc(`users/${target.uid}`).get()).data().accessDisabled,true);assert.equal((await db.doc(`staffDirectory/${target.uid}`).get()).data().active,false);
    if(role==='maintenance')assert.equal((await db.doc(`campusReports/${id}`).get()).data().assignedTo,replacement.uid);
    if(role==='vendor'){assert.equal((await db.doc(`canteens/${id}`).get()).data().vendorId,replacement.uid);assert.equal((await db.doc(`foodOrders/${id}`).get()).data().vendorId,replacement.uid);}
    if(role==='driver'){assert.equal((await db.doc(`buses/${id}`).get()).data().driverId,replacement.uid);assert.equal((await db.doc(`activeTrips/${id}`).get()).data().active,false);}
    if(role==='faculty'){assert.equal((await db.doc(`events/${id}`).get()).data().createdBy,replacement.uid);assert.equal((await db.doc(`eventRegistrations/${id}_student`).get()).data().organizerId,replacement.uid);}
    if(role==='mess')assert.equal((await db.doc(`messHalls/${id}`).get()).data().operatorId,replacement.uid);
  }
  assert.equal((await db.collection('roleAudit').where('action','==','STAFF_DEACTIVATED_AND_TRANSFERRED').get()).size,6);
});

test('staff lifecycle resumes partial deactivation and deletion without orphaning work',{skip:!enabled,timeout:180000},async()=>{
  const {initializeApp}=require('firebase-admin/app');const {getAuth}=require('firebase-admin/auth');const {getFirestore,Timestamp}=require('firebase-admin/firestore');
  const projectId=process.env.GCLOUD_PROJECT,app=initializeApp({projectId},`staff-resume-${Date.now()}`),auth=getAuth(app),db=getFirestore(app),suffix=Date.now(),password='EmulatorOnly123!';
  async function actor(role,label){const uid=`${label}_${suffix}`,email=`${uid}@kpriet.ac.in`;await auth.createUser({uid,email,password,emailVerified:true});await auth.setCustomUserClaims(uid,{role});await db.doc(`users/${uid}`).set({uid,email,name:label,role,department:'CSE',year:'',section:'',routeId:'',profileImage:'',notificationsEnabled:false,membershipGranted:true,membershipEmail:email,accessDisabled:false,createdAt:Timestamp.now()});await db.doc(`staffDirectory/${uid}`).set({uid,name:label,role,department:'CSE',team:'Test',active:true});const response=await fetch(`http://${process.env.FIREBASE_AUTH_EMULATOR_HOST}/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=demo-key`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email,password,returnSecureToken:true})});return {uid,token:(await response.json()).idToken};}
  async function call(user,name,data){const host=process.env.CIVORA_FUNCTIONS_EMULATOR_HOST||'127.0.0.1:5001';return (await fetch(`http://${host}/${projectId}/us-central1/${name}`,{method:'POST',headers:{'Content-Type':'application/json',Authorization:`Bearer ${user.token}`},body:JSON.stringify({data})})).json();}
  const admin=await actor('admin','resume_admin'),target=await actor('faculty','resume_old'),replacement=await actor('faculty','resume_new');
  await db.doc(`users/${target.uid}`).set({accessDisabled:true,deactivatedAt:Timestamp.now(),deactivatedBy:admin.uid},{merge:true});
  await db.doc(`staffDirectory/${target.uid}`).set({active:false,replacementUid:replacement.uid,deactivatedAt:Timestamp.now(),deactivatedBy:admin.uid},{merge:true});
  const resumed=await call(admin,'deactivateStaffAccount',{targetUid:target.uid,replacementUid:replacement.uid,confirm:true});
  assert.equal(resumed.result?.resumed,true,JSON.stringify(resumed));assert.equal((await auth.getUser(target.uid)).disabled,true);
  const deleted=await call(admin,'deleteDeactivatedStaffAccount',{targetUid:target.uid,confirmPhrase:'DELETE'});assert.equal(deleted.result?.deleted,true,JSON.stringify(deleted));
  await assert.rejects(auth.getUser(target.uid),error=>error.code==='auth/user-not-found');assert.equal((await db.doc(`users/${target.uid}`).get()).exists,false);
  assert.equal((await db.doc(`staffDirectory/${target.uid}`).get()).data().deletionStatus,'COMPLETED');assert.equal((await db.doc(`staffDeletions/${target.uid}`).get()).data().status,'COMPLETED');
  const retry=await call(admin,'deleteDeactivatedStaffAccount',{targetUid:target.uid,confirmPhrase:'DELETE'});assert.equal(retry.result?.deleted,true,JSON.stringify(retry));
});
