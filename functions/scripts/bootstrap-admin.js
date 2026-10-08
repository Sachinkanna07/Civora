// Trusted operator only. Existing Firebase Auth account required.
const {initializeApp}=require('firebase-admin/app');const {getAuth}=require('firebase-admin/auth');const {getFirestore}=require('firebase-admin/firestore');
const projectId=process.env.GCLOUD_PROJECT||process.env.GOOGLE_CLOUD_PROJECT;
if(!projectId)throw Error('Set GCLOUD_PROJECT to the explicitly confirmed Firebase project');
initializeApp({projectId});
(async()=>{
  const uid=process.argv[2];if(!uid)throw Error('Pass the confirmed existing administrator UID');
  const user=await getAuth().getUser(uid);const ref=getFirestore().doc(`users/${uid}`);
  if(!(await ref.get()).exists)throw Error('Register this account in Civora before bootstrapping it');
  await getAuth().setCustomUserClaims(uid,{...user.customClaims,role:'admin'});
  await ref.update({role:'admin'});
  if((await getAuth().getUser(uid)).customClaims?.role!=='admin')throw Error('Administrator claim verification failed');
  console.log('Administrator claim verified. Sign out and sign in again to refresh the app token.');
})().catch(e=>{console.error(e.message);process.exitCode=1});
