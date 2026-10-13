require('./local-environment');
const {test}=require('node:test');const assert=require('node:assert/strict');
const enabled=!!process.env.FIREBASE_AUTH_EMULATOR_HOST&&!!process.env.FIRESTORE_EMULATOR_HOST;
test('schedule conflicts serialize concurrent edits; event cancellation closes attendance',{skip:!enabled,timeout:180000},async()=>{
  const {initializeApp}=require('firebase-admin/app'),{getAuth}=require('firebase-admin/auth'),{getFirestore,Timestamp}=require('firebase-admin/firestore');
  const projectId=process.env.GCLOUD_PROJECT,app=initializeApp({projectId},'scheduling-test'),auth=getAuth(app),db=getFirestore(app),suffix=Date.now();
  async function actor(role){const uid=`schedule_${role}_${suffix}`,email=`${uid}@kpriet.ac.in`,password='EmulatorOnly123!';await auth.createUser({uid,email,password,emailVerified:true});await auth.setCustomUserClaims(uid,{role});await db.doc(`users/${uid}`).set({uid,email,name:role,role,department:`department_${suffix}`,year:'2',section:'A',routeId:'',profileImage:'',notificationsEnabled:false,membershipGranted:true,membershipEmail:email.toLowerCase(),createdAt:Timestamp.now()});const response=await fetch(`http://${process.env.FIREBASE_AUTH_EMULATOR_HOST}/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=demo-key`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email,password,returnSecureToken:true})});return {uid,token:(await response.json()).idToken};}
  const admin=await actor('admin'),faculty=await actor('faculty'),student=await actor('student');
  async function call(user,name,data){const host=process.env.CIVORA_FUNCTIONS_EMULATOR_HOST||'127.0.0.1:5001';return (await fetch(`http://${host}/${projectId}/us-central1/${name}`,{method:'POST',headers:{'Content-Type':'application/json',Authorization:`Bearer ${user.token}`},body:JSON.stringify({data}),signal:AbortSignal.timeout(90000)})).json();}
  const schedule={collection:'timetables',courseId:`course_${suffix}`,courseName:'Concurrent class',faculty:`Faculty ${suffix}`,room:`Room ${suffix}`,dayOfWeek:2,startMinute:600,endMinute:660,department:`department_${suffix}`,year:'2',section:'A',effectiveFrom:'2026-01-01',effectiveTo:'2027-12-31'};
  const race=await Promise.all(['first','second'].map(label=>call(admin,'manageCampusData',{...schedule,id:`${label}_${suffix}`})));
  assert.equal(race.filter(result=>!result.error).length,1,JSON.stringify(race));
  assert.equal((await call(admin,'manageCampusData',{...schedule,id:`adjacent_${suffix}`,startMinute:660,endMinute:720})).error,undefined);
  assert.ok((await call(student,'manageCampusData',schedule)).error);
  const override={collection:'academicOverrides',date:'2026-10-13',kind:'CLASS',baseCourseId:schedule.courseId,courseId:schedule.courseId,courseName:'Moved class',faculty:schedule.faculty,room:schedule.room,startMinute:720,endMinute:780,department:schedule.department,year:'2',section:'A',reason:'DEMO room revision'};
  assert.equal((await call(admin,'manageCampusData',{...override,id:`override_${suffix}`})).error,undefined);
  assert.ok((await call(admin,'manageCampusData',{...override,id:`override_conflict_${suffix}`,courseId:`other_${suffix}`})).error);
  assert.equal((await call(admin,'manageCampusData',{...override,id:`cancel_${suffix}`,kind:'CANCEL',reason:'DEMO holiday exception'})).error,undefined);
  const eventId=`cancelled_${suffix}`;
  await db.doc(`events/${eventId}`).set({title:'Cancelled event',createdBy:faculty.uid,capacity:1,registeredCount:0,registrationEnabled:true,startTime:Timestamp.fromMillis(Date.now()+600000),endTime:Timestamp.fromMillis(Date.now()+3600000)});
  assert.equal((await call(student,'eventRegistration',{eventId})).error,undefined);
  assert.ok((await call(student,'setContentStatus',{kind:'event',id:eventId,action:'CANCEL'})).error);
  assert.equal((await call(faculty,'setContentStatus',{kind:'event',id:eventId,action:'CANCEL'})).error,undefined);
  const ticket=await db.doc(`eventRegistrations/${eventId}_${student.uid}`).get();
  assert.ok((await call(faculty,'checkIn',{registrationId:ticket.id,token:ticket.data().token})).error);
  assert.ok((await call(faculty,'setContentStatus',{kind:'event',id:eventId,action:'RESTORE'})).error);
  const deadlineEvent=`deadline_${suffix}`;
  await db.doc(`events/${deadlineEvent}`).set({title:'Closed registration',createdBy:faculty.uid,capacity:0,registeredCount:0,registrationEnabled:true,startTime:Timestamp.fromMillis(Date.now()+600000),registrationDeadline:Timestamp.fromMillis(Date.now()-1000)});
  assert.ok((await call(student,'eventRegistration',{eventId:deadlineEvent})).error);
});
