require('./local-environment');
const {test}=require('node:test'),assert=require('node:assert/strict');
const {institutionalEmail,eligible,hasCampusAccess}=require('../admission-policy');
test('proposed retention settings do not enable production deletion',()=>{
  const {deletionEnabled}=require('../deletion-policy');
  assert.equal(deletionEnabled(false),false);
  assert.equal(deletionEnabled(true),true);
});
test('institutional domain is exact and grants no role from an email prefix',()=>{
  assert.equal(institutionalEmail(' Faculty@KPRIET.AC.IN '),true);
  for(const email of ['person@evil.kpriet.ac.in','person@kpriet.ac.in.evil','person@other.ac.in','a@b@kpriet.ac.in'])assert.equal(institutionalEmail(email),false);
  const user={email:'admin@kpriet.ac.in',emailVerified:true,disabled:false};
  assert.equal(eligible(user,{email:user.email,email_verified:true},{}),true);
  assert.equal(user.customClaims,undefined);
});
test('exceptions require current verified identity and the exact approved email',()=>{
  const user={email:'operator@example.com',emailVerified:true,disabled:false},token={email:user.email,email_verified:true};
  const profile={role:'student',admissionException:true,admissionEmail:user.email,membershipGranted:true,membershipEmail:user.email};
  assert.equal(hasCampusAccess(user,token,profile),true);
  assert.equal(eligible({...user,emailVerified:false},token,profile),false);
  assert.equal(eligible(user,{...token,email_verified:false},profile),false);
  assert.equal(eligible(user,{...token,email:'old@example.com'},profile),false);
  assert.equal(eligible({...user,email:'changed@example.com'},{...token,email:'changed@example.com'},profile),false);
  assert.equal(hasCampusAccess(user,token,{...profile,membershipGranted:false}),false);
  assert.equal(hasCampusAccess(user,token,{...profile,accessDisabled:true}),false);
  assert.equal(hasCampusAccess(user,token,{...profile,role:'faculty'}),false);
});
