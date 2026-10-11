require('./local-environment');
const {test}=require('node:test');
const assert=require('node:assert/strict');
const enabled=!!process.env.FIREBASE_AUTH_EMULATOR_HOST&&!!process.env.FIRESTORE_EMULATOR_HOST&&!!process.env.FIREBASE_STORAGE_EMULATOR_HOST;
test('production hardening: bounded rush concurrency, stale claims, abuse limits and durable deletion',{skip:!enabled,timeout:300000},async()=>{
  const {initializeApp}=require('firebase-admin/app');
  const {getAuth}=require('firebase-admin/auth');
  const {getFirestore,Timestamp}=require('firebase-admin/firestore');
  const {getStorage}=require('firebase-admin/storage');
  const projectId=process.env.GCLOUD_PROJECT,app=initializeApp({projectId,storageBucket:`${projectId}.appspot.com`},'completion-tests');
  const db=getFirestore(app),auth=getAuth(app),suffix=`completion_${Date.now()}`;
  async function actor(role,label=role){
    const uid=`${label}_${suffix}`,email=`${uid}@kpriet.ac.in`,password='EmulatorOnly123!';
    await auth.createUser({uid,email,password,emailVerified:true});await auth.setCustomUserClaims(uid,{role});
    await db.doc(`users/${uid}`).set({uid,email,name:label,role,department:'CSE',year:'2',section:'A',routeId:'',profileImage:'',notificationsEnabled:false,membershipGranted:true,membershipEmail:email.toLowerCase(),createdAt:Timestamp.now()});
    const response=await fetch(`http://${process.env.FIREBASE_AUTH_EMULATOR_HOST}/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=demo-key`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email,password,returnSecureToken:true})});
    const body=await response.json();assert.ok(body.idToken);return {uid,token:body.idToken};
  }
  async function call(user,name,data){
    const host=process.env.CIVORA_FUNCTIONS_EMULATOR_HOST||'127.0.0.1:5001';
    const response=await fetch(`http://${host}/${projectId}/us-central1/${name}`,{method:'POST',headers:{'Content-Type':'application/json',Authorization:`Bearer ${user.token}`},body:JSON.stringify({data}),signal:AbortSignal.timeout(120000)});
    return response.json();
  }
  const student=await actor('student'),vendor=await actor('vendor'),admin=await actor('admin'),deleting=await actor('student','deleting');
  const canteenId=`canteen_${suffix}`,menuItemId=`menu_${suffix}`;
  await db.doc(`canteens/${canteenId}`).set({name:'Concurrent kitchen',vendorId:vendor.uid,open:true});
  await db.doc(`menuItems/${menuItemId}`).set({name:'Meal',canteenId,pricePaise:5000,available:true});
  const cart=i=>({canteenId,requestId:`rush_${i}`,items:[{menuItemId,quantity:1}],expectedTotalPaise:5000});
  const started=Date.now();
  // Distinct actors model 50 students rather than forcing one user's quota hot spot.
  const students=[];
  for(let i=0;i<50;i+=5)students.push(...await Promise.all(Array.from({length:5},(_,offset)=>actor('student',`rush${i+offset}`))));
  const rush=[];
  for(let i=0;i<50;i+=5)rush.push(...await Promise.all(students.slice(i,i+5).map((user,offset)=>call(user,'placeFoodOrder',cart(i+offset)))));
  for(const result of rush)assert.equal(result.error,undefined,JSON.stringify(result));
  assert.equal(new Set(rush.map(x=>x.result.orderId)).size,50);
  const repeats=await Promise.all(Array.from({length:8},()=>call(student,'placeFoodOrder',cart('same'))));
  for(const result of repeats)assert.equal(result.error,undefined,JSON.stringify(result));
  assert.equal(new Set(repeats.map(x=>x.result.orderId)).size,1);
  console.log(JSON.stringify({scenario:'50 orders with five concurrent requests per batch',elapsedMs:Date.now()-started,orders:50,concurrency:5,environment:'local emulator; includes fixture setup; not production latency'}));
  // Server-side current claims override a still-valid privileged ID token.
  await auth.setCustomUserClaims(vendor.uid,{role:'student'});
  assert.equal((await call(vendor,'updateFoodOrder',{orderId:rush[0].result.orderId,status:'ACCEPTED'})).error?.status,'PERMISSION_DENIED');
  await db.doc(`users/${student.uid}`).update({accessDisabled:true});
  assert.equal((await call(student,'placeFoodOrder',cart('disabled'))).error?.status,'PERMISSION_DENIED');
  await db.doc(`users/${student.uid}`).update({accessDisabled:false});
  const {execFileSync}=require('node:child_process');
  execFileSync(process.execPath,['scripts/set-account-access.js',student.uid,'disable'],{env:process.env});
  assert.equal((await auth.getUser(student.uid)).disabled,true);
  assert.equal((await db.doc(`users/${student.uid}`).get()).data().accessDisabled,true);
  execFileSync(process.execPath,['scripts/set-account-access.js',student.uid,'enable'],{env:process.env});
  assert.equal((await auth.getUser(student.uid)).disabled,false);
  assert.equal((await db.doc(`users/${student.uid}`).get()).data().accessDisabled,false);
  assert.equal((await call(admin,'getOperationsSummary',{})).error,undefined); // warm discovery before seeding a minute-bound quota
  const minute=Math.floor(Date.now()/60000);
  const batch=db.batch();for(let shard=0;shard<16;shard++)batch.set(db.doc(`requestLimits/${admin.uid}_getOperationsSummary_${shard}`),{window:minute,count:10});await batch.commit();
  assert.equal((await call(admin,'getOperationsSummary',{})).error?.status,'RESOURCE_EXHAUSTED');
  assert.equal((await call(deleting,'requestAccountDeletion',{confirm:false})).error?.status,'INVALID_ARGUMENT');
  const ownEvent=`event_${suffix}`;
  await db.doc(`events/${ownEvent}`).set({title:'Deletion capacity',createdBy:admin.uid,capacity:2,registeredCount:1,registrationEnabled:true,startTime:Timestamp.fromMillis(Date.now()+600000)});
  await db.doc(`eventRegistrations/${ownEvent}_${deleting.uid}`).set({eventId:ownEvent,userId:deleting.uid,attended:false,token:'a'.repeat(48)});
  await db.doc(`notifications/deleting_${suffix}`).set({userId:deleting.uid,body:'private'});
  await db.doc(`mealPasses/${deleting.uid}`).set({userId:deleting.uid,token:'private'});
  await db.doc(`users/${deleting.uid}/devices/token`).set({token:'private'});
  await getStorage(app).bucket().file(`reports/${deleting.uid}/draft/photo.jpg`).save(Buffer.from([255,216,255,217]),{contentType:'image/jpeg'});
  const deletion=await call(deleting,'requestAccountDeletion',{confirm:true});assert.equal(deletion.result?.accepted,true,JSON.stringify(deletion));
  const deadline=Date.now()+90000;
  while(Date.now()<deadline&&(await db.doc(`accountDeletions/${deleting.uid}`).get()).data()?.status!=='COMPLETED')await new Promise(resolve=>setTimeout(resolve,500));
  assert.equal((await db.doc(`accountDeletions/${deleting.uid}`).get()).data().status,'COMPLETED');
  await assert.rejects(auth.getUser(deleting.uid),e=>e.code==='auth/user-not-found');
  assert.equal((await db.doc(`users/${deleting.uid}`).get()).exists,false);
  assert.equal((await db.collection(`users/${deleting.uid}/devices`).get()).size,0);
  assert.equal((await db.doc(`events/${ownEvent}`).get()).data().registeredCount,0);
  assert.equal((await db.collection('notifications').where('userId','==',deleting.uid).get()).size,0);
  assert.equal((await getStorage(app).bucket().getFiles({prefix:`reports/${deleting.uid}/`}))[0].length,0);
  assert.ok((await call(deleting,'getMealPass',{messId:'missing'})).error);
  assert.equal((await db.doc(`users/${student.uid}`).get()).exists,true,'unrelated account is preserved');
});
