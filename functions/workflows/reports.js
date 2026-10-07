const {onCall,HttpsError,db,serverTime,requireAuth,text,id,fail,record,Timestamp,getAuth,randomBytes,foodTransitions,reportTransitions,canTransition,hasCapacity,priceOrder} = require('../shared');
exports.submitReport = onCall(async req => {
  const {uid} = requireAuth(req, ['student']); const d = req.data; const ref = db.doc(`campusReports/${uid}_${id(d.requestId)}`);
  const category = text(d.category, 30); if (!['ELECTRICAL','WATER','CLEANLINESS','SAFETY','INFRASTRUCTURE','TRANSPORT','OTHER'].includes(category)) fail('Invalid category.');
  const report = {title: text(d.title, 120), description: text(d.description, 4000), category, location: text(d.location || '', 200, false), imageUrl: text(d.imageUrl || '', 2000, false), createdBy: uid, status: 'SUBMITTED', assignedTo: '', resolutionNote: '', createdAt: serverTime(), updatedAt: serverTime()};
  if (report.imageUrl && !/^https:\/\//.test(report.imageUrl)) fail('Image must be an HTTPS URL.');
  await db.runTransaction(async tx => { const prior = await tx.get(ref); if (prior.exists) return; tx.set(ref, report); record(tx, uid, {title: 'Report submitted', body: report.title, type: 'REPORT', status: 'SUBMITTED', destination: 'REPORT', entityId: ref.id}); });
  return {reportId: ref.id};
});
exports.updateReport = onCall(async req => {
  requireAuth(req, ['admin']); const ref = db.doc(`campusReports/${id(req.data.reportId)}`); const next = text(req.data.status, 30);
  const note = text(req.data.resolutionNote || '', 2000, false); const assignedTo = text(req.data.assignedTo || '', 128, false);
  await db.runTransaction(async tx => { const snap = await tx.get(ref); if (!snap.exists) fail('Report not found.'); const r = snap.data();
    if (!canTransition(reportTransitions, r.status, next)) fail('Invalid report transition.');
    if (next === 'ASSIGNED' && !assignedTo) fail('Select an assignee.');
    if (['RESOLVED','REJECTED'].includes(next) && !note) fail('Add a resolution note.');
    tx.update(ref, {status: next, assignedTo: assignedTo || r.assignedTo, resolutionNote: note, updatedAt: serverTime()});
    record(tx, r.createdBy, {title: `Report ${next.toLowerCase().replaceAll('_', ' ')}`, body: r.title, type: 'REPORT', status: next, destination: 'REPORT', entityId: ref.id});
  }); return {success: true};
});
