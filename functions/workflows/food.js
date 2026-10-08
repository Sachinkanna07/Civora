const {onCall,HttpsError,db,serverTime,requireAuth,text,id,fail,record,Timestamp,getAuth,randomBytes,foodTransitions,reportTransitions,canTransition,hasCapacity,priceOrder} = require('../shared');
exports.placeFoodOrder = onCall(async req => {
  const {uid} = requireAuth(req, ['student']); const canteenId = id(req.data.canteenId); const requestId = id(req.data.requestId);
  const cart = req.data.items; if (!Array.isArray(cart) || cart.length < 1 || cart.length > 30) fail('Cart is empty or too large.');
  cart.forEach(x => { if (!x || typeof x !== 'object') throw new HttpsError('invalid-argument', 'Invalid cart item.'); id(x.menuItemId); }); const orderRef = db.doc(`foodOrders/${uid}_${requestId}`);
  return db.runTransaction(async tx => {
    const prior = await tx.get(orderRef); if (prior.exists) return {orderId: prior.id, token: prior.data().token};
    const canteen = await tx.get(db.doc(`canteens/${canteenId}`)); if (!canteen.exists || !canteen.data().open) fail('Canteen is closed.');
    const snapshots = await Promise.all(cart.map(x => tx.get(db.doc(`menuItems/${x.menuItemId}`))));
    const menu = new Map(snapshots.filter(s => s.exists).map(s => [s.id, s.data()]));
    if (snapshots.some(s => !s.exists || s.data().canteenId !== canteenId)) fail('Items must belong to this canteen.');
    let items; try { items = priceOrder(menu, cart); } catch (_) { fail('Menu changed. Review your cart.'); }
    const totalPaise = items.reduce((total, x) => total + x.pricePaise * x.quantity, 0); const token = orderRef.id.slice(-8).toUpperCase();
    tx.set(orderRef, {userId: uid, canteenId, vendorId: canteen.data().vendorId, items, totalPaise, token, status: 'PLACED', paymentState: 'PAY_AT_COUNTER', createdAt: serverTime(), updatedAt: serverTime()});
    record(tx, uid, {title: 'Food order placed', body: `Token ${token}`, type: 'FOOD_ORDER', status: 'PLACED', destination: 'FOOD', entityId: orderRef.id});
    return {orderId: orderRef.id, token};
  });
});
exports.updateFoodOrder = onCall(async req => {
  const {uid, role} = requireAuth(req, ['vendor', 'admin', 'student']); const ref = db.doc(`foodOrders/${id(req.data.orderId)}`); const next = text(req.data.status, 30);
  return db.runTransaction(async tx => {
    const snap = await tx.get(ref); if (!snap.exists) fail('Order not found.'); const o = snap.data();
    if (role === 'student' ? o.userId !== uid || o.status !== 'PLACED' || next !== 'CANCELLED' : role !== 'admin' && o.vendorId !== uid) throw new HttpsError('permission-denied', 'This order is not assigned to you.');
    if (!canTransition(foodTransitions, o.status, next)) fail('Order status has changed. Refresh and retry.');
    tx.update(ref, {status: next, updatedAt: serverTime()});
    record(tx, o.userId, {title: `Food order ${next.toLowerCase()}`, body: `Token ${o.token}`, type: 'FOOD_ORDER', status: next, destination: 'FOOD', entityId: ref.id});
    return {success: true};
  });
});
