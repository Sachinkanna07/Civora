const {onCall,HttpsError,db,serverTime,requireAuth,text,id,fail,record,Timestamp,getAuth,randomBytes,foodTransitions,reportTransitions,canTransition,hasCapacity,priceOrder} = require('../shared');
exports.setMenuAvailability = onCall(async req => {
  const {uid, role} = requireAuth(req, ['vendor','admin']); const ref = db.doc(`menuItems/${id(req.data.menuItemId)}`);
  if (typeof req.data.available !== 'boolean') fail('Invalid availability.');
  await db.runTransaction(async tx => { const snap = await tx.get(ref); if (!snap.exists) fail('Item missing.'); const canteen = await tx.get(db.doc(`canteens/${snap.data().canteenId}`));
    if (!canteen.exists || (role !== 'admin' && canteen.data().vendorId !== uid)) throw new HttpsError('permission-denied','Menu is not assigned to you.');
    tx.update(ref, {available: req.data.available}); }); return {success: true};
});
