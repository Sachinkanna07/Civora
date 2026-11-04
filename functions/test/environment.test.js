require('./local-environment');
const {test}=require('node:test');
const assert=require('node:assert/strict');
test('integration execution is isolated and CI cannot silently skip emulator coverage',()=>{
  const hosts=['FIREBASE_AUTH_EMULATOR_HOST','FIRESTORE_EMULATOR_HOST','FIREBASE_STORAGE_EMULATOR_HOST'];
  if(hosts.some(name=>process.env[name])) {
    assert.match(process.env.GCLOUD_PROJECT||'',/^demo-/, 'Integration tests require an isolated demo project');
    for(const name of hosts)assert.match(process.env[name]||'',/^(localhost|127\.0\.0\.1):\d+$/, `${name} must be local`);
  }
  if(process.env.CIVORA_REQUIRE_EMULATORS==='true')for(const name of hosts)assert.ok(process.env[name],`${name} is required in CI`);
});
