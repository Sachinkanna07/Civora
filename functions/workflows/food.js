const {onCall,HttpsError,db,serverTime,requireAuth,text,id,fail,record,Timestamp,getAuth,randomBytes,foodTransitions,reportTransitions,canTransition,hasCapacity,priceOrder} = require('../shared');
exports.placeFoodOrder = onCall('placeFoodOrder',async req => {
  const {uid} = requireAuth(req, ['student']); const canteenId = id(req.data.canteenId); const requestId = id(req.data.requestId);
  const cart = req.data.items; if (!Array.isArray(cart) || cart.length < 1 || cart.length > 30) fail('Cart is empty or too large.');
  cart.forEach(x => { if (!x || typeof x !== 'object') throw new HttpsError('invalid-argument', 'Invalid cart item.'); id(x.menuItemId); }); const orderRef = db.doc(`foodOrders/${uid}_${requestId}`);
  return db.runTransaction(async tx => {
    await require('../shared').requireActiveAccount(tx,uid);
    const prior = await tx.get(orderRef); if (prior.exists) return {orderId: prior.id, token: prior.data().token};
    const canteen = await tx.get(db.doc(`canteens/${canteenId}`)); if (!canteen.exists || !canteen.data().open || canteen.data().paused === true) fail('This canteen is not taking new orders. Your cart is saved.');
    const snapshots = await Promise.all(cart.map(x => tx.get(db.doc(`menuItems/${x.menuItemId}`))));
    const menu = new Map(snapshots.filter(s => s.exists).map(s => [s.id, s.data()]));
    if (snapshots.some(s => !s.exists || s.data().canteenId !== canteenId)) fail('Items must belong to this canteen.');
    let items; try { items = priceOrder(menu, cart); } catch (_) { fail('Menu changed. Review your cart.'); }
    const totalPaise = items.reduce((total, x) => total + x.pricePaise * x.quantity, 0); const token = orderRef.id.slice(-8).toUpperCase();
    if(req.data.expectedTotalPaise !== undefined && req.data.expectedTotalPaise !== totalPaise)fail('Menu prices changed. Review your refreshed cart before checking out.');
    tx.set(orderRef, {userId: uid, canteenId, canteenName:canteen.data().name, vendorId: canteen.data().vendorId, items, totalPaise, token, preparationMinutes:canteen.data().preparationMinutes||null, status: 'PLACED', paymentState: 'PAY_AT_COUNTER', createdAt: serverTime(), updatedAt: serverTime()});
    record(tx, uid, {title: 'Food order placed', body: `Token ${token}`, type: 'FOOD_ORDER', status: 'PLACED', destination: 'FOOD', entityId: orderRef.id});
    record(tx, canteen.data().vendorId, {title:'New kitchen order',body:`Token ${token} · ${items.reduce((n,x)=>n+x.quantity,0)} items`,type:'FOOD_ORDER',status:'PLACED',destination:'FOOD',entityId:orderRef.id});
    return {orderId: orderRef.id, token};
  });
});
exports.updateFoodOrder = onCall('updateFoodOrder',async req => {
  const {uid, role} = requireAuth(req, ['vendor', 'admin', 'student']); const ref = db.doc(`foodOrders/${id(req.data.orderId,257)}`); const next = text(req.data.status, 30);
  return db.runTransaction(async tx => {
    await require('../shared').requireActiveAccount(tx,uid);
    const snap = await tx.get(ref); if (!snap.exists) fail('Order not found.'); const o = snap.data();
    if (role === 'student' ? o.userId !== uid || o.status !== 'PLACED' || next !== 'CANCELLED' : role !== 'admin' && o.vendorId !== uid) throw new HttpsError('permission-denied', 'This order is not assigned to you.');
    if (!canTransition(foodTransitions, o.status, next)) fail('Order status has changed. Refresh and retry.');
    tx.update(ref, {status: next, updatedAt: serverTime()});
    record(tx, o.userId, {title: `Food order ${next.toLowerCase()}`, body: `Token ${o.token}`, type: 'FOOD_ORDER', status: next, destination: 'FOOD', entityId: ref.id});
    return {success: true};
  });
});
