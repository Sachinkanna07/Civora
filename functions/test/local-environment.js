// Import before any SDK or destructive fixture operation.
const assert=require('node:assert/strict');
const names=['FIREBASE_AUTH_EMULATOR_HOST','FIRESTORE_EMULATOR_HOST','FIREBASE_STORAGE_EMULATOR_HOST'];
if(names.some(name=>process.env[name])) {
  assert.match(process.env.GCLOUD_PROJECT||'',/^demo-/,'Emulator tests require an explicit isolated demo project');
  for(const name of names)if(process.env[name])assert.match(process.env[name],/^(localhost|127\.0\.0\.1):\d+$/,'Test endpoints must be loopback');
}
if(process.env.CIVORA_REQUIRE_EMULATORS==='true')for(const name of names)assert.ok(process.env[name],`${name} is required`);
