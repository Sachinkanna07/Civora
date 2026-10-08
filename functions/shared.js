const {onCall, HttpsError} = require('firebase-functions/v2/https');
const {initializeApp} = require('firebase-admin/app');
const {getFirestore, FieldValue, Timestamp} = require('firebase-admin/firestore');
const {getAuth} = require('firebase-admin/auth');
const {randomBytes} = require('node:crypto');
const {foodTransitions, reportTransitions, canTransition, hasCapacity, priceOrder} = require('./policy');
initializeApp();
const db = getFirestore();
const serverTime = () => FieldValue.serverTimestamp();
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
function id(value) { const v = text(value, 128); if (!/^[a-zA-Z0-9_-]+$/.test(v)) throw new HttpsError('invalid-argument', 'Invalid reference.'); return v; }
function fail(message) { throw new HttpsError('failed-precondition', message); }
function record(tx, uid, data) {
  const base = {userId: uid, createdAt: serverTime(), ...data};
  tx.set(db.collection('activities').doc(), base);
  tx.set(db.collection('notifications').doc(), {...base, category: 'RELEVANT', read: false});
}

module.exports = {onCall,HttpsError,db,serverTime,requireAuth,text,id,fail,record,Timestamp,getAuth,randomBytes,foodTransitions,reportTransitions,canTransition,hasCapacity,priceOrder};
