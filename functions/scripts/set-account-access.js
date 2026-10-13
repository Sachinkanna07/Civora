// Trusted operator only; no credentials are accepted through arguments.
const {initializeApp}=require('firebase-admin/app');
const {getAuth}=require('firebase-admin/auth');
const {getFirestore,FieldValue}=require('firebase-admin/firestore');
const projectId=process.env.GCLOUD_PROJECT||process.env.GOOGLE_CLOUD_PROJECT;
if(!projectId)throw Error('Set GCLOUD_PROJECT to the explicitly confirmed Firebase project');
const [uid,action]=process.argv.slice(2);
if(!/^[A-Za-z0-9_-]{1,128}$/.test(uid||'')||!['disable','enable'].includes(action))throw Error('Usage: set-account-access.js UID disable|enable');
initializeApp({projectId});
(async()=>{
  const db=getFirestore(),auth=getAuth(),ref=db.doc(`users/${uid}`);
  const user=await auth.getUser(uid);
  if(action==='disable') {
    // Block rules and callables before Auth's cached tokens expire.
    await ref.update({accessDisabled:true});
    await auth.updateUser(uid,{disabled:true});
    await auth.revokeRefreshTokens(uid);
  } else {
    const [profile,deletion]=await Promise.all([ref.get(),db.doc(`accountDeletions/${uid}`).get()]);
    if(deletion.exists||!profile.exists)throw Error('Deleted/deleting or missing profiles cannot be enabled');
    if(profile.data().role!==(user.customClaims?.role||'student'))throw Error('Resolve profile and trusted claim role mismatch first');
    await auth.updateUser(uid,{disabled:false});
    await ref.update({accessDisabled:false});
  }
  await db.collection('roleAudit').add({target:uid,action:`ACCESS_${action.toUpperCase()}`,actor:'trusted-operator',createdAt:FieldValue.serverTimestamp()});
  console.log(`Campus access ${action} completed. Re-enabled users must sign in again.`);
})().catch(error=>{console.error(error.message);process.exitCode=1});
