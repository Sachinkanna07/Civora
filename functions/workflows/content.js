const {onCall,HttpsError,db,serverTime,requireAuth,text,id,fail,record,Timestamp,getAuth,randomBytes,foodTransitions,reportTransitions,canTransition,hasCapacity,priceOrder} = require('../shared');
exports.publishCampusContent = onCall(async req => {
  const {uid, role} = requireAuth(req, ['faculty','admin']); const kind = req.data.kind; const d = req.data;
  const profile = await db.doc(`users/${uid}`).get(); const department = role === 'faculty' ? profile.data()?.department || '' : text(d.department || '', 80, false);
  if (role === 'faculty' && !department) fail('Faculty department must be assigned by an administrator.');
  let content;
  if (kind === 'announcement') {
    if(d.expiresAt && (!Number.isSafeInteger(d.expiresAt) || d.expiresAt <= Date.now()))fail('Expiry must be a future date.');
    const category = text(d.category || 'General',30); const priority = text(d.priority || 'normal',30);
    if (!['General','Academic','Department','Urgent'].includes(category) || !['normal','high','urgent'].includes(priority)) fail('Invalid category or priority.');
    if (role !== 'admin' && (priority === 'urgent' || category === 'Urgent')) throw new HttpsError('permission-denied','Only administrators can publish emergency notices.');
    content = {title: text(d.title,120), body: text(d.body,4000), category, priority, department, audience: 'students', authorId: uid, authorName: profile.data()?.name || '', createdAt: serverTime(), expiresAt: d.expiresAt ? Timestamp.fromMillis(d.expiresAt) : null};
  } else if (kind === 'event') {
    if (!Number.isSafeInteger(d.startTime) || !Number.isSafeInteger(d.endTime) || d.endTime <= d.startTime || d.startTime <= Date.now() || !Number.isInteger(d.capacity) || d.capacity < 0 || d.capacity > 100000) fail('Check event times and capacity.');
    content = {title: text(d.title,120), description: text(d.body,4000), category: text(d.category || 'Campus',30), department, venue: text(d.venue,200), organizer: profile.data()?.name || '', createdBy: uid, startTime: Timestamp.fromMillis(d.startTime), endTime: Timestamp.fromMillis(d.endTime), capacity: d.capacity, registrationEnabled: true, registeredCount: 0, imageUrl: text(d.imageUrl || '',2000,false), createdAt: serverTime(), clubId: text(d.clubId || '',128,false)};
  } else fail('Invalid content type.');
  if(content.imageUrl && !/^https:\/\//.test(content.imageUrl))fail('Image must be an HTTPS URL.');
  const collection = kind === 'event' ? 'events' : 'announcements'; const ref = d.id ? db.doc(`${collection}/${id(d.id)}`) : db.collection(collection).doc();
  await db.runTransaction(async tx => { const old = await tx.get(ref);
    if (old.exists) {
      const author = kind === 'event' ? old.data().createdBy : old.data().authorId;
      if (role !== 'admin' && author !== uid) throw new HttpsError('permission-denied','You do not own this content.');
      if (kind === 'event' && d.capacity !== 0 && d.capacity < (old.data().registeredCount || 0)) fail('Capacity cannot be less than registrations.');
      if (kind === 'event') { content.registeredCount = old.data().registeredCount || 0; content.createdBy = old.data().createdBy; }
      else { content.authorId = old.data().authorId; content.authorName = old.data().authorName; }
      content.createdAt = old.data().createdAt;
    }
    tx.set(ref, content);
  }); return {id: ref.id};
});
