require('./local-environment');
const {test,before,after,beforeEach} = require('node:test');
const fs=require('node:fs');
const {initializeTestEnvironment,assertSucceeds,assertFails} = require('@firebase/rules-unit-testing');
const {doc,setDoc,deleteDoc,updateDoc,getDoc,collection,getDocs,query,where,orderBy,limit,serverTimestamp,Timestamp} = require('firebase/firestore');
const enabled=!!process.env.FIRESTORE_EMULATOR_HOST;
let env;
before(async()=>{if(enabled)env=await initializeTestEnvironment({projectId:process.env.GCLOUD_PROJECT||'demo-civora',firestore:{rules:fs.readFileSync('../firestore.rules','utf8')}})});
after(async()=>{if(env)await env.cleanup()});
beforeEach(async()=>{if(env){await env.clearFirestore();for(const [uid,role] of [['s','student'],['other','student'],['v','vendor'],['wrong','vendor'],['d','driver'],['f','faculty']])await seed(`users/${uid}`,{role});}});
const secured=(name,fn)=>test(name,{skip:!enabled},fn);
const user=(uid,role)=>env.authenticatedContext(uid,{email:`${uid}@kpriet.ac.in`,email_verified:true,...(role?{role}: {})}).firestore();
const profile=uid=>({uid,name:'Campus User',email:`${uid}@kpriet.ac.in`,role:'student',department:'CSE',year:'2',section:'A',routeId:'',profileImage:'',notificationsEnabled:true,createdAt:serverTimestamp()});
async function seed(path,value){if(path.startsWith('users/'))value={membershipGranted:true,membershipEmail:`${path.split('/')[1].toLowerCase()}@kpriet.ac.in`,...value};await env.withSecurityRulesDisabled(async ctx=>setDoc(doc(ctx.firestore(),path),value))}
secured('public registration cannot assign privileged roles or extra fields',async()=>{
 await env.withSecurityRulesDisabled(ctx=>deleteDoc(doc(ctx.firestore(),'users/s')));
 const db=user('s');await assertSucceeds(setDoc(doc(db,'users/s'),profile('s')));
 await seed('users/s',{...profile('s'),createdAt:Timestamp.now()});
 await assertFails(setDoc(doc(db,'users/s'),{...profile('s'),role:'admin'}));
 await assertFails(updateDoc(doc(db,'users/s'),{role:'faculty'}));
 await assertFails(updateDoc(doc(db,'users/s'),{isAdmin:true}));
 await assertFails(updateDoc(doc(db,'users/s'),{email_verified:true}));
 await assertFails(updateDoc(doc(db,'users/s'),{accessDisabled:false}));
 await assertFails(updateDoc(doc(db,'users/s'),{name:'x'.repeat(101)}));
 await assertSucceeds(updateDoc(doc(db,'users/s'),{department:'ECE'}));
 await assertFails(getDoc(doc(user('other'),'users/s')));
 await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(),'users/s')));
});
secured('protected profile role and access state revoke cached privileged tokens',async()=>{
 await seed('users/revoked',{role:'admin'});
 await seed('users/private',{name:'Private user'});
 const stale=user('revoked','admin');
 await assertSucceeds(getDoc(doc(stale,'users/private')));
 await seed('users/revoked',{role:'student'});
 await assertFails(getDoc(doc(stale,'users/private')));
 await seed('users/revoked',{role:'admin',accessDisabled:true});
 await assertFails(getDoc(doc(stale,'users/private')));
 await assertFails(getDoc(doc(stale,'users/revoked')));
 await assertFails(updateDoc(doc(stale,'users/revoked'),{accessDisabled:false}));
});
secured('onboarding is own-only; verification and server membership are required for campus access',async()=>{
 await seed('events/admission_rule',{title:'Campus event'});
 const pending=env.authenticatedContext('pending',{email:'pending@kpriet.ac.in',email_verified:false}).firestore();
 await assertSucceeds(setDoc(doc(pending,'users/pending'),profile('pending')));
 await assertSucceeds(getDoc(doc(pending,'users/pending')));
 await assertFails(getDoc(doc(pending,'events/admission_rule')));
 await assertFails(updateDoc(doc(pending,'users/pending'),{membershipGranted:true,membershipEmail:'pending@kpriet.ac.in'}));
 await seed('users/pending',{...profile('pending'),createdAt:Timestamp.now()});
 await assertFails(getDoc(doc(pending,'events/admission_rule')));
 const verified=user('pending');
 await assertSucceeds(getDoc(doc(verified,'events/admission_rule')));
 await seed('users/pending',{...profile('pending'),createdAt:Timestamp.now(),membershipGranted:false});
 await assertFails(getDoc(doc(verified,'events/admission_rule')));
 await assertSucceeds(getDoc(doc(verified,'users/pending')));
});
secured('client cannot forge workflows, prices or capacity counters',async()=>{
 const db=user('s');for(const path of ['events/e','eventRegistrations/e_s','foodOrders/o','campusReports/r','activities/a','announcements/a'])await assertFails(setDoc(doc(db,path),{userId:'s',role:'admin'}));
 await seed('events/e',{registeredCount:0});await assertFails(updateDoc(doc(db,'events/e'),{registeredCount:1}));
});
secured('orders scoped to student and assigned vendor queries',async()=>{
 await seed('foodOrders/a',{userId:'s',vendorId:'v',createdAt:Timestamp.now()});
 await assertSucceeds(getDocs(query(collection(user('s'),'foodOrders'),where('userId','==','s'),orderBy('createdAt','desc'),limit(100))));
 await assertSucceeds(getDoc(doc(user('v','vendor'),'foodOrders/a')));
 await assertFails(getDoc(doc(user('wrong','vendor'),'foodOrders/a')));
 await assertFails(getDocs(collection(user('s'),'foodOrders')));
 await assertFails(updateDoc(doc(user('v','vendor'),'foodOrders/a'),{status:'COMPLETED'}));
});
secured('notification read state only, own inbox query',async()=>{
 await seed('notifications/n',{userId:'s',title:'Update',body:'Body',type:'REPORT',status:'SUBMITTED',destination:'REPORT',entityId:'r',createdAt:Timestamp.now(),category:'RELEVANT',read:false});
 await assertSucceeds(updateDoc(doc(user('s'),'notifications/n'),{read:true}));
 await assertFails(updateDoc(doc(user('s'),'notifications/n'),{body:'forged'}));
 await assertFails(updateDoc(doc(user('other'),'notifications/n'),{read:true}));
 await assertSucceeds(getDocs(query(collection(user('s'),'notifications'),where('userId','==','s'),orderBy('createdAt','desc'),limit(100))));
});
secured('follow ownership, immutable creation time and parent existence',async()=>{
 await seed('clubs/c',{name:'Club'});const db=user('s');
 await assertSucceeds(setDoc(doc(db,'clubFollowers/c_s'),{userId:'s',clubId:'c',createdAt:serverTimestamp()}));
 await assertFails(setDoc(doc(db,'clubFollowers/missing_s'),{userId:'s',clubId:'missing',createdAt:serverTimestamp()}));
 await assertFails(updateDoc(doc(db,'clubFollowers/c_s'),{userId:'other'}));
});
secured('driver can only publish assigned active trip with throttle and valid coordinates',async()=>{
 await seed('users/other',{role:'driver'});
 await seed('buses/b',{driverId:'d'});
 await seed('activeTrips/b',{busId:'b',routeId:'r',driverId:'d',active:true,startedAt:Timestamp.now(),updatedAt:Timestamp.now()});
 const location={latitude:12,longitude:80,accuracy:20,locationUpdatedAt:serverTimestamp(),locationCapturedAt:Timestamp.now()};
 await assertFails(updateDoc(doc(user('other','driver'),'activeTrips/b'),location));
 await assertFails(updateDoc(doc(user('d','driver'),'activeTrips/b'),{...location,latitude:100}));
 await assertSucceeds(updateDoc(doc(user('d','driver'),'activeTrips/b'),location));
 await assertFails(updateDoc(doc(user('d','driver'),'activeTrips/b'),location));
 await assertFails(updateDoc(doc(user('d','driver'),'activeTrips/b'),{active:false}));
});
secured('faculty attendance restricted to own events',async()=>{
 await seed('users/other',{role:'faculty'});
 await seed('eventRegistrations/e_s',{userId:'s',organizerId:'f',eventId:'e'});
 await assertSucceeds(getDoc(doc(user('f','faculty'),'eventRegistrations/e_s')));
 await assertFails(getDoc(doc(user('other','faculty'),'eventRegistrations/e_s')));
 await assertFails(updateDoc(doc(user('f','faculty'),'eventRegistrations/e_s'),{attended:true}));
});
