// Fictional campus fixtures. Refuses production credentials and endpoints.
const {initializeApp}=require('firebase-admin/app');
const {getAuth}=require('firebase-admin/auth');
const {getFirestore,Timestamp}=require('firebase-admin/firestore');
const projectId=process.env.GCLOUD_PROJECT||'demo-civora';
if(!/^demo-civora(?:-v2)?$/.test(projectId))throw Error('Only isolated Civora emulator projects may be seeded.');
for(const variable of ['FIREBASE_AUTH_EMULATOR_HOST','FIRESTORE_EMULATOR_HOST']) {
  if(!/^(127\.0\.0\.1|localhost):\d+$/.test(process.env[variable]||''))
    throw Error(`Set ${variable} to a loopback emulator endpoint. Production seeding is forbidden.`);
}
initializeApp({projectId,storageBucket:`${projectId}.appspot.com`});
const auth=getAuth(),db=getFirestore();
const now=Timestamp.now();
async function createOnly(path,data){const ref=db.doc(path);await db.runTransaction(async tx=>{if(!(await tx.get(ref)).exists)tx.create(ref,{...data,demo:true});});}
(async()=>{
  for(const role of ['student','faculty','admin','vendor','driver','mess','maintenance']) {
    const uid=`demo_${role}`,email=`${role}@demo.example.com`;
    let user;try{user=await auth.getUser(uid);}catch(e){if(e.code!=='auth/user-not-found')throw e;user=await auth.createUser({uid,email,password:'DemoCampus123!',emailVerified:true,displayName:`Demo ${role}`});}
    if(user.email!==email)throw Error(`Existing emulator account ${uid} does not match the fixture`);
    await auth.setCustomUserClaims(uid,{...user.customClaims,role});
    // Do not add demo metadata to profiles: client rules use an exact field schema.
    const ref=db.doc(`users/${uid}`);
    if(!(await ref.get()).exists)await ref.create({uid,email,name:`Demo ${role}`,role,department:'DEMO CSE',year:'2',section:'A',routeId:'demo_route',boardingStop:'DEMO Library',profileImage:'',notificationsEnabled:false,admissionException:true,admissionEmail:email,membershipGranted:true,membershipEmail:email,createdAt:now});
    if(role!=='student')await createOnly(`staffDirectory/${uid}`,{uid,name:`Demo ${role}`,role,department:'DEMO CSE',team:'Demo campus operations',active:true});
  }
  await createOnly('clubs/demo_club',{name:'DEMO Robotics Club',description:'Fictional campus club for local demonstration.',category:'Technology',imageUrl:''});
  await createOnly('departments/demo_cse',{code:'DEMO CSE',name:'DEMO Computer Science',years:['1','2','3','4'],sections:['A','B']});
  await createOnly('messHalls/demo_mess',{name:'DEMO Campus Mess',operatorId:'demo_mess',open:true});
  await createOnly('mealEntitlements/demo_mess_demo_student',{messId:'demo_mess',userId:'demo_student',active:true});
  await createOnly('mealPeriods/demo_lunch',{messId:'demo_mess',label:'DEMO Lunch',opensAt:Timestamp.fromMillis(Date.now()-600000),closesAt:Timestamp.fromMillis(Date.now()+7200000),createdAt:now});
  await createOnly('announcements/demo_notice',{title:'DEMO Campus welcome',body:'Fictional campus information. This is a local emulator demonstration.',category:'Academic',priority:'normal',audience:'students',department:'DEMO CSE',authorId:'demo_faculty',authorName:'Demo faculty',createdAt:now,expiresAt:null});
  await createOnly('events/demo_event',{title:'DEMO Robotics Workshop',description:'Fictional workshop for demonstrating RSVP and QR tickets.',category:'Workshop',department:'DEMO CSE',venue:'DEMO Innovation Lab',organizer:'Demo faculty',createdBy:'demo_faculty',clubId:'demo_club',startTime:Timestamp.fromMillis(Date.now()+1800000),endTime:Timestamp.fromMillis(Date.now()+7200000),capacity:20,registeredCount:0,registrationEnabled:true,imageUrl:'',createdAt:now});
  for(let day=1;day<=7;day++)await createOnly(`timetables/demo_class_${day}`,{courseId:'demo_course',courseName:'DEMO Software Engineering',faculty:'Demo faculty',dayOfWeek:day,startMinute:540,endMinute:600,room:'DEMO Lab 1',department:'DEMO CSE',year:'2',section:'A'});
  await createOnly('canteens/demo_canteen',{name:'DEMO Campus Canteen',vendorId:'demo_vendor',open:true});
  await createOnly('menuItems/demo_meal',{name:'DEMO Vegetable Meal',canteenId:'demo_canteen',pricePaise:6500,available:true,imageUrl:''});
  await createOnly('menuItems/demo_tea',{name:'DEMO Tea',canteenId:'demo_canteen',pricePaise:1500,available:true,imageUrl:''});
  await createOnly('busRoutes/demo_route',{name:'DEMO Campus Loop (fictional)',stops:[{name:'DEMO Library',latitude:12.9,longitude:80.1},{name:'DEMO Main Gate',latitude:12.91,longitude:80.11}]});
  await createOnly('buses/demo_bus',{name:'DEMO Bus 1',routeId:'demo_route',driverId:'demo_driver'});
  await createOnly('campusLocations/demo_library',{name:'DEMO Library',category:'Academic',description:'Fictional demonstration location.',latitude:12.9,longitude:80.1});
  // No GPS, trips, orders, reports or tickets are fabricated.
  console.log('Local DEMO fixtures ready. Accounts: <role>@demo.example.com / DemoCampus123!');
})().catch(e=>{console.error(e.message);process.exitCode=1});
