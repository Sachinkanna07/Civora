// Trusted operator only. Existing Firebase Auth account required.
const {initializeApp}=require('firebase-admin/app');const {getAuth}=require('firebase-admin/auth');const {getFirestore}=require('firebase-admin/firestore');
initializeApp({projectId:process.env.GCLOUD_PROJECT||process.env.GOOGLE_CLOUD_PROJECT});
(async()=>{const uid=process.argv[2];if(!uid)throw Error('Pass the existing administrator UID');const user=await getAuth().getUser(uid);await getAuth().setCustomUserClaims(uid,{...user.customClaims,role:'admin'});await getFirestore().doc(`users/${uid}`).update({role:'admin'});console.log('Administrator provisioned. Sign in again to refresh claims.');})().catch(e=>{console.error(e.message);process.exitCode=1});
