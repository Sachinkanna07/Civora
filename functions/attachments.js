const {getStorage} = require('firebase-admin/storage');
const {HttpsError} = require('firebase-functions/v2/https');
const {onCall,db,requireAuth,Timestamp}=require('./shared');
const MAX_IMAGE_BYTES = 3 * 1024 * 1024;
function isValidJpeg(bytes) {
  if(bytes.length < 20 || bytes[0]!==0xff || bytes[1]!==0xd8 || bytes[bytes.length-2]!==0xff || bytes[bytes.length-1]!==0xd9)return false;
  let position=2,dimensions=false;
  while(position+4<bytes.length) {
    if(bytes[position++]!==0xff)return false;
    while(position<bytes.length&&bytes[position]===0xff)position++;
    if(position+3>=bytes.length)return false;
    const marker=bytes[position++];
    if(marker===0xda)return dimensions;
    const length=bytes.readUInt16BE(position);if(length<2||position+length>bytes.length)return false;
    if([0xc0,0xc1,0xc2].includes(marker)) {
      if(length<8)return false;const height=bytes.readUInt16BE(position+3),width=bytes.readUInt16BE(position+5);
      if(bytes[position+2]!==8||width<1||height<1||width>1600||height>1600)return false;dimensions=true;
    }
    position+=length;
  }return false;
}
async function validateAttachment(path, prefix) {
  if (!path) return '';
  if (typeof path !== 'string' || path.length > 500 || !path.startsWith(prefix) || path.includes('..') || !/^[a-zA-Z0-9_/-]+\.jpg$/.test(path))
    throw new HttpsError('invalid-argument', 'Choose a photo uploaded for this record.');
  let metadata;
  try { [metadata] = await getStorage().bucket().file(path).getMetadata(); }
  catch (_) { throw new HttpsError('failed-precondition', 'Photo upload is incomplete. Retry the upload.'); }
  if (metadata.contentType !== 'image/jpeg' || Number(metadata.size) <= 0 || Number(metadata.size) > MAX_IMAGE_BYTES)
    throw new HttpsError('invalid-argument', 'Photos must be JPEG images smaller than 3 MB.');
  const [bytes]=await getStorage().bucket().file(path).download();
  if(!isValidJpeg(bytes))throw new HttpsError('invalid-argument','Photo could not be verified. Choose a JPEG photo up to 1600 pixels wide and high.');
  // Firebase uploads may mint bearer download tokens. Strip them before an
  // attachment is linked to a private report; client metadata updates are denied.
  if(prefix.startsWith('reports/')||prefix.startsWith('reportEvidence/'))
    await getStorage().bucket().file(path).setMetadata({metadata:{...(metadata.metadata||{}),firebaseStorageDownloadTokens:null}});
  return path;
}
exports.authorizePhotoUpload=onCall('authorizePhotoUpload',async req=>{
  const {uid,role}=requireAuth(req);const path=req.data.path;
  if(typeof path!=='string'||path.length>500||path.includes('..'))throw new HttpsError('invalid-argument','Choose a valid photo destination.');
  let permitId,allowed=false;
  let match=path.match(/^reports\/([A-Za-z0-9_-]+)\/([A-Za-z0-9_-]+)\/photo\.jpg$/);
  if(match&&role==='student'&&match[1]===uid){permitId=`${uid}_report_${match[2]}`;allowed=true;}
  match=path.match(/^reportEvidence\/([A-Za-z0-9_-]{1,257})\/([A-Za-z0-9_-]+)\/([A-Za-z0-9_-]+)\.jpg$/);
  if(match&&role==='maintenance'&&match[2]===uid){
    const report=await db.doc(`campusReports/${match[1]}`).get();
    allowed=report.exists&&report.data().assignedTo===uid&&['ACCEPTED','IN_PROGRESS'].includes(report.data().status);
    permitId=`${uid}_evidence_${match[3]}.jpg`;
  }
  match=path.match(/^campusImages\/([A-Za-z0-9_-]+)\/([A-Za-z0-9_-]+)\.jpg$/);
  if(match&&['faculty','admin'].includes(role)&&match[1]===uid){permitId=`${uid}_content_${match[2]}.jpg`;allowed=true;}
  if(!allowed||!permitId)throw new HttpsError('permission-denied','This account cannot upload to that record.');
  const permit=db.doc(`uploadPermits/${permitId}`),day=new Date().toISOString().slice(0,10),budget=db.doc(`uploadBudgets/${uid}_${day}`);
  await db.runTransaction(async tx=>{
    const [prior,usage]=await Promise.all([tx.get(permit),tx.get(budget)]);if(prior.exists&&prior.data().path===path)return;
    const count=usage.data()?.count||0;if(count>=20)throw new HttpsError('resource-exhausted','Daily photo limit reached. Contact campus operations if more evidence is required.');
    tx.set(budget,{uid,day,count:count+1,expiresAt:Timestamp.fromMillis(Date.now()+3*86400000)},{merge:true});
    tx.create(permit,{uid,path,expiresAt:Timestamp.fromMillis(Date.now()+60*60000)});
  });
  return {path};
});
module.exports.validateAttachment=validateAttachment;
module.exports.MAX_IMAGE_BYTES=MAX_IMAGE_BYTES;
module.exports.isValidJpeg=isValidJpeg;
