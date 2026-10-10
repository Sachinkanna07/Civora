const {getStorage} = require('firebase-admin/storage');
const {HttpsError} = require('firebase-functions/v2/https');
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
module.exports = {validateAttachment, MAX_IMAGE_BYTES, isValidJpeg};
