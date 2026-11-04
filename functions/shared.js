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
function onCall(name, handler, {requiresAdmission=true}={}) {
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
    if(profile.data()?.role&&profile.data().role!==(user.customClaims?.role||'student'))throw new HttpsError('permission-denied','Your role changed. Sign in again.');
    if (deletion.exists) throw new HttpsError('failed-precondition','Account deletion is in progress.');
    if(requiresAdmission&&!require('./admission-policy').hasCampusAccess(user,req.auth.token,profile.data()))
      throw new HttpsError('permission-denied','Verify your email and obtain campus admission before using campus services.');
    const policy = CALL_LIMITS[name] || {max:60,windowMs:60000};
    const window = Math.floor(Date.now()/policy.windowMs);
    // One counter serializes only repeated calls from the same account and
    // action. It gives an exact limit without creating a campus-wide hot spot.
    const ref = db.doc(`requestLimits/${req.auth.uid}_${name}_${window}`);
    await db.runTransaction(async tx => {
      const old = await tx.get(ref); const count = old.data()?.count || 0;
      if (count >= policy.max) throw new HttpsError('resource-exhausted',policy.message || 'Too many requests. Wait a moment and try again.');
      tx.set(ref,{window,count:count+1,expiresAt:Timestamp.fromMillis(Date.now()+Math.max(policy.windowMs*2,86400000))});
    });
    return handler(req);
  });
}
const CALL_LIMITS = Object.freeze({
  activateCampusAccess:{max:6,windowMs:15*60000,message:'Too many access checks. Wait 15 minutes before trying again.'},
  lookupAdmissionCandidate:{max:30,windowMs:60000},
  invitePrivilegedUser:{max:10,windowMs:60*60000},
  // Allows a short order rush and idempotent retries while bounding abuse.
  placeFoodOrder:{max:30,windowMs:60000},
  submitReport:{max:6,windowMs:60*60000,message:'Report limit reached. Wait before submitting another report.'},
  authorizePhotoUpload:{max:24,windowMs:24*60*60000,message:'Daily photo limit reached. Contact campus operations if more evidence is required.'},
  publishTripLocation:{max:8,windowMs:60000},
  reportTransportIssue:{max:12,windowMs:60*60000},
  publishCampusContent:{max:20,windowMs:60*60000},
  deactivateStaffAccount:{max:10,windowMs:60*60000},
  deleteDeactivatedStaffAccount:{max:5,windowMs:60*60000},
});
function requireAuth(req, roles) {
  if (!req.auth) throw new HttpsError('unauthenticated', 'Sign in to continue.');
  const role = req.auth.token.role || 'student';
  if(!['student','faculty','admin','vendor','driver','maintenance','mess'].includes(role))throw new HttpsError('permission-denied','Your campus role needs administrator review.');
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
