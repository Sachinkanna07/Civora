require('./local-environment');
const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs');
const {isValidJpeg}=require('../attachments');
test('JPEG validation rejects spoofed MIME payloads, truncated data and oversized dimensions',()=>{
  assert.equal(isValidJpeg(Buffer.from('not an image')),false);
  assert.equal(isValidJpeg(Buffer.from([255,216,255,217])),false);
  const image=Buffer.from(fs.readFileSync('test/fixtures/jpeg.base64','utf8'),'base64');assert.equal(isValidJpeg(image),true);
  assert.equal(isValidJpeg(image.subarray(0,image.length-5)),false);
  const oversized=Buffer.from(image);const sof=oversized.indexOf(Buffer.from([255,192]));assert.ok(sof>=0);oversized.writeUInt16BE(1601,sof+7);assert.equal(isValidJpeg(oversized),false);
});
