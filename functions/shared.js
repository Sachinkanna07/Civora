const {onCall: firebaseOnCall, HttpsError} = require('firebase-functions/v2/https');
const {initializeApp} = require('firebase-admin/app');
const {getFirestore, FieldValue, Timestamp} = require('firebase-admin/firestore');
const {getAuth} = require('firebase-admin/auth');
const {randomBytes} = require('node:crypto');
const {foodTransitions, reportTransitions, canTransition, hasCapacity, priceOrder} = require('./policy');
initializeApp();
const db = getFirestore();
const serverTime = () => FieldValue.serverTimestamp();
// Apply the same live identity, deletion and abuse checks to every callable.
// Emulator attestation is deliberately bypassed; production fails closed.
function onCall(name, handler) {
  if (!/^[A-Za-z][A-Za-z0-9_]{0,79}$/.test(name) || typeof handler !== 'function') throw new Error('Callable identity is required');
  return firebaseOnCall({enforceAppCheck: process.env.FUNCTIONS_EMULATOR !== 'true', maxInstances: 10}, async req => {
    requireAuth(req);
    const user = await getAuth().getUser(req.auth.uid).catch(() => { throw new HttpsError('unauthenticated','Sign in again.'); });
    if (user.disabled || (user.customClaims?.role || 'student') !== (req.auth.token.role || 'student'))
      throw new HttpsError('permission-denied','Your access changed. Sign in again.');
    const revokedAfter = Date.parse(user.tokensValidAfterTime || '') / 1000;
    if (Number.isFinite(revokedAfter) && req.auth.token.auth_time < revokedAfter)
      throw new HttpsError('unauthenticated','Your session expired. Sign in again.');
    const [deletion,profile] = await Promise.all([db.doc(`accountDeletions/${req.auth.uid}`).get(),db.doc(`users/${req.auth.uid}`).get()]);
    if (profile.data()?.accessDisabled === true) throw new HttpsError('permission-denied','Your campus access is disabled.');
    if (deletion.exists) throw new HttpsError('failed-precondition','Account deletion is in progress.');
    const window = Math.floor(Date.now()/60000);
    // Server-selected shards avoid serializing a lunch rush on one document.
    // Sixteen shards with ten requests each give a hard 160/minute upper bound.
    const shard = randomBytes(1)[0] % 16;
    const ref = db.doc(`requestLimits/${req.auth.uid}_${name}_${shard}`);
    await db.runTransaction(async tx => {
      const old = await tx.get(ref); const count = old.data()?.window === window ? old.data().count : 0;
      if (count >= 10) throw new HttpsError('resource-exhausted','Too many requests. Try again in a minute.');
      tx.set(ref,{window,count:count+1,expiresAt:Timestamp.fromMillis(Date.now()+86400000)});
    });
    return handler(req);
  });
}
function requireAuth(req, roles) {
  if (!req.auth) throw new HttpsError('unauthenticated', 'Sign in to continue.');
  const role = req.auth.token.role || 'student';
  if (roles && !roles.includes(role)) throw new HttpsError('permission-denied', 'Your role cannot perform this action.');
  if (!req.data || typeof req.data !== 'object' || Array.isArray(req.data)) throw new HttpsError('invalid-argument', 'Expected form fields.');
  return {uid: req.auth.uid, role};
}
function text(value, max = 200, required = true) {
  if (typeof value !== 'string' || value.length > max || (required && !value.trim())) throw new HttpsError('invalid-argument', 'Check the form fields.');
  return value.trim();
}
function id(value, max = 128) { const v = text(value, max); if (!/^[a-zA-Z0-9_-]+$/.test(v)) throw new HttpsError('invalid-argument', 'Invalid reference.'); return v; }
function fail(message) { throw new HttpsError('failed-precondition', message); }
function record(tx, uid, data) {
  const base = {userId: uid, createdAt: serverTime(), ...data};
  tx.set(db.collection('activities').doc(), base);
  tx.set(db.collection('notifications').doc(), {...base, category: 'RELEVANT', read: false});
}

async function requireActiveAccount(tx, uid) {
  if ((await tx.get(db.doc(`accountDeletions/${uid}`))).exists)
    throw new HttpsError('failed-precondition','Account deletion is in progress.');
}

module.exports = {onCall,HttpsError,db,serverTime,requireAuth,requireActiveAccount,text,id,fail,record,Timestamp,getAuth,randomBytes,foodTransitions,reportTransitions,canTransition,hasCapacity,priceOrder};
