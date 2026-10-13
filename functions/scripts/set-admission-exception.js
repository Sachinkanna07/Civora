// Institutional approval required. This changes admission, never verified-email or roles.
const {initializeApp}=require('firebase-admin/app');
const {getAuth}=require('firebase-admin/auth');
const {getFirestore,FieldValue}=require('firebase-admin/firestore');
const projectId=process.env.GCLOUD_PROJECT||process.env.GOOGLE_CLOUD_PROJECT;
const [uid,action,reference]=process.argv.slice(2);
if(!projectId||!/^[A-Za-z0-9_-]{1,128}$/.test(uid||'')||!['approve','revoke'].includes(action)||!reference?.trim()||reference.length>300)
  throw Error('Set the explicit GCLOUD_PROJECT; usage: set-admission-exception.js UID approve|revoke APPROVAL_REFERENCE');
initializeApp({projectId});
(async()=>{
  const user=await getAuth().getUser(uid),db=getFirestore(),ref=db.doc(`users/${uid}`),approve=action==='approve';
  await db.runTransaction(async tx=>{
    if((await tx.get(db.doc(`accountDeletions/${uid}`))).exists)throw Error('Deleting accounts cannot receive admission changes');
    if(!(await tx.get(ref)).exists)throw Error('A Civora profile is required');
    tx.update(ref,{admissionException:approve,admissionEmail:approve?user.email.trim().toLowerCase():'',membershipGranted:false,membershipEmail:''});
    tx.set(db.collection('roleAudit').doc(),{actor:'trusted-operator',target:uid,action:approve?'ADMISSION_EXCEPTION_APPROVED':'ADMISSION_EXCEPTION_REVOKED',approvalReference:reference.trim(),createdAt:FieldValue.serverTimestamp()});
  });
  console.log('Admission exception updated. Firebase email verification and fresh campus activation are still required.');
})().catch(error=>{console.error(error.message);process.exitCode=1});
