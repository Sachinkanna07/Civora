require('./local-environment');
const {test}=require('node:test'),assert=require('node:assert/strict');
const enabled=!!process.env.FIREBASE_AUTH_EMULATOR_HOST&&!!process.env.FIRESTORE_EMULATOR_HOST;
test('email verification activates exact-domain members without assigning roles; approved exceptions remain revocable',{skip:!enabled,timeout:300000},async()=>{
  const {initializeApp}=require('firebase-admin/app'),{getAuth}=require('firebase-admin/auth'),{getFirestore,Timestamp}=require('firebase-admin/firestore');
  const projectId=process.env.GCLOUD_PROJECT,app=initializeApp({projectId},'admission-tests'),auth=getAuth(app),db=getFirestore(app),suffix=Date.now();
  const authHost=process.env.FIREBASE_AUTH_EMULATOR_HOST,functionsHost=process.env.CIVORA_FUNCTIONS_EMULATOR_HOST||'127.0.0.1:5001';
  async function rest(path,data){const response=await fetch(`http://${authHost}/identitytoolkit.googleapis.com/v1/${path}?key=demo-key`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(data),signal:AbortSignal.timeout(30000)});const body=await response.json();assert.equal(body.error,undefined,JSON.stringify(body));return body;}
  async function signin(user){user.token=(await rest('accounts:signInWithPassword',{email:user.email,password:user.password,returnSecureToken:true})).idToken;return user;}
  async function actor(label,domain,verified=false,role='student',admitted=false){
    const uid=`admission_${label}_${suffix}`,email=`${uid}@${domain}`,password='EmulatorOnly123!';
    await auth.createUser({uid,email,password,emailVerified:verified});
    if(role!=='student')await auth.setCustomUserClaims(uid,{role});
    await db.doc(`users/${uid}`).set({uid,email,name:label,role,department:'CSE',year:'2',section:'A',routeId:'',profileImage:'',notificationsEnabled:false,createdAt:Timestamp.now(),...(admitted?{membershipGranted:true,membershipEmail:email}: {})});
    return signin({uid,email,password});
  }
  async function call(user,name,data={}){const response=await fetch(`http://${functionsHost}/${projectId}/us-central1/${name}`,{method:'POST',headers:{'Content-Type':'application/json',Authorization:`Bearer ${user.token}`},body:JSON.stringify({data}),signal:AbortSignal.timeout(120000)});return response.json();}
  const member=await actor('admin_like_name','kpriet.ac.in');
  assert.equal((await call(member,'activateCampusAccess')).error?.status,'PERMISSION_DENIED');
  assert.equal((await call(member,'getOperationsSummary')).error?.status,'PERMISSION_DENIED');
  await rest('accounts:sendOobCode',{requestType:'VERIFY_EMAIL',idToken:member.token});
  const codes=await (await fetch(`http://${authHost}/emulator/v1/projects/${projectId}/oobCodes`)).json();
  const code=codes.oobCodes.find(item=>item.email===member.email&&item.requestType==='VERIFY_EMAIL');assert.ok(code);
  await rest('accounts:update',{oobCode:code.oobCode});
  // A stale unverified ID token must not become admitted merely because Auth changed.
  assert.equal((await call(member,'activateCampusAccess')).error?.status,'PERMISSION_DENIED');
  await signin(member);
  assert.equal((await call(member,'activateCampusAccess')).result?.admitted,true);
  assert.equal((await auth.getUser(member.uid)).customClaims?.role,undefined);
  assert.equal((await db.doc(`users/${member.uid}`).get()).data().role,'student');
  assert.equal((await call(member,'getOperationsSummary')).error?.status,'PERMISSION_DENIED');
  const admin=await actor('approved_admin','kpriet.ac.in',true,'admin',true),outsider=await actor('outside_operator','example.com',true);
  const pendingVendor=await actor('pending_vendor','kpriet.ac.in',false,'vendor');
  const canteenId=`pending_kitchen_${suffix}`,menuItemId=`pending_meal_${suffix}`;
  await db.doc(`canteens/${canteenId}`).set({name:'Pending kitchen fixture',vendorId:pendingVendor.uid,open:true});
  await db.doc(`menuItems/${menuItemId}`).set({name:'Fixture meal',canteenId,pricePaise:1000,available:true});
  assert.equal((await call(member,'placeFoodOrder',{canteenId,requestId:'pending_vendor',items:[{menuItemId,quantity:1}]})).error?.status,'FAILED_PRECONDITION');
  assert.equal((await call(outsider,'activateCampusAccess')).error?.status,'PERMISSION_DENIED');
  assert.equal((await call(member,'setAdmissionException',{userId:outsider.uid,approve:true,approvalReference:'Forged self approval'})).error?.status,'PERMISSION_DENIED');
  assert.equal((await call(admin,'lookupAdmissionCandidate',{email:outsider.email})).result?.uid,outsider.uid);
  assert.equal((await call(admin,'setAdmissionException',{userId:outsider.uid,approve:true,approvalReference:'Local institutional approval fixture'})).error,undefined);
  assert.equal((await db.doc(`users/${outsider.uid}`).get()).data().membershipGranted,false);
  assert.equal((await call(outsider,'activateCampusAccess')).result?.admitted,true);
  const eventId=`admission_event_${suffix}`;
  await db.doc(`events/${eventId}`).set({title:'Admission fixture',createdBy:admin.uid,capacity:2,registeredCount:0,registrationEnabled:true,startTime:Timestamp.fromMillis(Date.now()+3600000),endTime:Timestamp.fromMillis(Date.now()+7200000)});
  assert.equal((await call(member,'eventRegistration',{eventId})).error,undefined);
  assert.equal((await call(outsider,'eventRegistration',{eventId})).error,undefined);
  assert.equal((await call(admin,'setAdmissionException',{userId:outsider.uid,approve:false,approvalReference:'Local revocation fixture'})).error,undefined);
  assert.equal((await call(outsider,'eventRegistration',{eventId})).error?.status,'PERMISSION_DENIED');
  assert.equal((await call(outsider,'activateCampusAccess')).error?.status,'PERMISSION_DENIED');
});
