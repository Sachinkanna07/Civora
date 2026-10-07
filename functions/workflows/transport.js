const {onCall,HttpsError,db,serverTime,requireAuth,text,id,fail,record,Timestamp,getAuth,randomBytes,foodTransitions,reportTransitions,canTransition,hasCapacity,priceOrder} = require('../shared');
exports.manageTrip = onCall(async req => {
  const {uid} = requireAuth(req, ['driver']); const busId = id(req.data.busId); const ref = db.doc(`activeTrips/${busId}`);
  await db.runTransaction(async tx => { const [bus, trip] = await Promise.all([tx.get(db.doc(`buses/${busId}`)), tx.get(ref)]);
    if (!bus.exists || bus.data().driverId !== uid) throw new HttpsError('permission-denied','Bus is not assigned to you.');
    if (typeof req.data.active !== 'boolean') fail('Invalid trip state.');
    if (req.data.active) { if (trip.exists && trip.data().active) return; tx.set(ref, {busId, routeId: bus.data().routeId, driverId: uid, active: true, startedAt: serverTime(), updatedAt: serverTime()}); }
    else { if (trip.exists) tx.update(ref, {active: false, endedAt: serverTime(), updatedAt: serverTime()}); }
  }); return {success: true};
});
