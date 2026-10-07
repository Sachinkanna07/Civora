const {onCall,HttpsError,db,serverTime,requireAuth,text,id,fail,record,Timestamp,getAuth,randomBytes,foodTransitions,reportTransitions,canTransition,hasCapacity,priceOrder} = require('../shared');
exports.eventRegistration = onCall(async req => {
  const {uid} = requireAuth(req, ['student']); const eventId = id(req.data.eventId); const cancel = req.data.cancel === true;
  const eventRef = db.doc(`events/${eventId}`); const regRef = db.doc(`eventRegistrations/${eventId}_${uid}`);
  const token = randomBytes(24).toString('hex');
  return db.runTransaction(async tx => {
    const [eventSnap, regSnap] = await Promise.all([tx.get(eventRef), tx.get(regRef)]);
    if (!eventSnap.exists) fail('Event no longer exists.'); const e = eventSnap.data();
    if (cancel) {
      if (!regSnap.exists) return {registered: false};
      if (regSnap.data().attended) fail('Attendance has already been recorded.');
      tx.delete(regRef); tx.update(eventRef, {registeredCount: Math.max(0, (e.registeredCount || 0) - 1)});
      record(tx, uid, {title: 'Event registration cancelled', body: e.title, type: 'EVENT_CANCELLED', status: 'CANCELLED', destination: 'EVENT', entityId: eventId});
      return {registered: false};
    }
    if (regSnap.exists) return {registered: true, token: regSnap.data().token};
    if (!hasCapacity(e) || !e.startTime || e.startTime.toMillis() <= Date.now()) fail('Registration closed or event is full.');
    tx.set(regRef, {eventId, userId: uid, token, attended: false, organizerId: e.createdBy, createdAt: serverTime()});
    tx.update(eventRef, {registeredCount: (e.registeredCount || 0) + 1});
    record(tx, uid, {title: 'Registered for event', body: e.title, type: 'EVENT_REGISTERED', status: 'REGISTERED', destination: 'EVENT', entityId: eventId});
    return {registered: true, token};
  });
});
exports.checkIn = onCall(async req => {
  const {uid, role} = requireAuth(req, ['faculty', 'admin']);
  const registrationId = id(req.data.registrationId); const token = text(req.data.token, 128);
  return db.runTransaction(async tx => {
    const ref = db.doc(`eventRegistrations/${registrationId}`); const reg = await tx.get(ref);
    if (!reg.exists || reg.data().token !== token) fail('Ticket is invalid.'); const r = reg.data();
    const event = await tx.get(db.doc(`events/${r.eventId}`));
    if (!event.exists || (role !== 'admin' && event.data().createdBy !== uid)) throw new HttpsError('permission-denied', 'You do not manage this event.');
    const start = event.data().startTime?.toMillis(); const end = event.data().endTime?.toMillis();
    if (!start || !end || Date.now() < start - 3600000 || Date.now() > end + 3600000) fail('Check-in is outside the attendance window.');
    if (r.attended) fail('Ticket already used.');
    tx.update(ref, {attended: true, attendedAt: serverTime(), checkedBy: uid});
    tx.set(db.doc(`eventAttendance/${registrationId}`), {eventId: r.eventId, userId: r.userId, organizerId: event.data().createdBy, checkedBy: uid, createdAt: serverTime()});
    record(tx, r.userId, {title: 'Event attended', body: event.data().title, type: 'EVENT_ATTENDED', status: 'ATTENDED', destination: 'EVENT', entityId: r.eventId});
    return {success: true};
  });
});
