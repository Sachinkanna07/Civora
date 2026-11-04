const {onCall,HttpsError,db,serverTime,requireAuth,text,id,fail,record,Timestamp,getAuth,randomBytes,foodTransitions,reportTransitions,canTransition,hasCapacity,priceOrder} = require('../shared');
exports.manageTrip = onCall('manageTrip',async req => {
  const {uid} = requireAuth(req, ['driver']); const busId = id(req.data.busId); const ref = db.doc(`activeTrips/${busId}`);
  await db.runTransaction(async tx => { const [bus, trip] = await Promise.all([tx.get(db.doc(`buses/${busId}`)), tx.get(ref)]);
    if (!bus.exists || bus.data().driverId !== uid) throw new HttpsError('permission-denied','Bus is not assigned to you.');
    if (typeof req.data.active !== 'boolean') fail('Invalid trip state.');
    if (req.data.active) { if (trip.exists && trip.data().active) return; tx.set(ref, {busId, routeId: bus.data().routeId, driverId: uid, active: true, startedAt: serverTime(), updatedAt: serverTime()}); }
    else { if (trip.exists) tx.update(ref, {active: false, endedAt: serverTime(), updatedAt: serverTime()}); }
  }); return {success: true};
});
exports.reportTransportIssue = onCall('reportTransportIssue',async req=>{
  const {uid}=requireAuth(req,['driver']),d=req.data,busId=id(d.busId),kind=text(d.kind,30),requestId=id(d.requestId);
  if(!['DELAY','BREAKDOWN','DISRUPTION','CLEARED'].includes(kind))fail('Choose a transport update.');
  const minutes=d.delayMinutes||0;if(!Number.isInteger(minutes)||minutes<0||minutes>180)fail('Delay must be between 0 and 180 minutes.');
  const note=text(d.note||'',500,false),ref=db.doc(`transportUpdates/${busId}_${requestId}`);
  await db.runTransaction(async tx=>{const [bus,prior]=await Promise.all([tx.get(db.doc(`buses/${busId}`)),tx.get(ref)]);
    if(!bus.exists||bus.data().driverId!==uid)throw new HttpsError('permission-denied','Vehicle is not assigned to you.');
    if(prior.exists)return;
    tx.set(ref,{busId,busName:bus.data().name,routeId:bus.data().routeId,driverId:uid,kind,delayMinutes:minutes,note,createdAt:serverTime()});
    tx.set(db.collection('operationsAudit').doc(),{actor:uid,type:'TRANSPORT',entityId:busId,kind,note,createdAt:serverTime()});
  });return {success:true};
});

exports.publishTripLocation = onCall('publishTripLocation',async req=>{
  const {uid}=requireAuth(req,['driver']),d=req.data,busId=id(d.busId);
  const latitude=d.latitude,longitude=d.longitude,accuracy=d.accuracy,capturedAt=d.capturedAt;
  if(!Number.isFinite(latitude)||latitude < -90||latitude > 90||!Number.isFinite(longitude)||longitude < -180||longitude > 180||!Number.isFinite(accuracy)||accuracy < 0||accuracy > 200||!Number.isSafeInteger(capturedAt)||capturedAt < Date.now()-120000||capturedAt > Date.now()+10000)fail('Location is inaccurate or stale. Wait for a fresh GPS fix.');
  const source=d.locationSource||'GPS';
  if(!['GPS','SIMULATED'].includes(source)||(source==='SIMULATED'&&!process.env.GCLOUD_PROJECT?.startsWith('demo-')))fail('Simulated location is only allowed in an isolated demo project.');
  await db.runTransaction(async tx=>{
    const ref=db.doc(`activeTrips/${busId}`),[bus,trip]=await Promise.all([tx.get(db.doc(`buses/${busId}`)),tx.get(ref)]);
    if(!bus.exists||bus.data().driverId!==uid||!trip.exists||trip.data().driverId!==uid||!trip.data().active)throw new HttpsError('permission-denied','Trip is stopped or no longer assigned to you.');
    if(trip.data().locationCapturedAt?.toMillis()>=capturedAt)return;
    tx.update(ref,{latitude,longitude,accuracy,locationCapturedAt:Timestamp.fromMillis(capturedAt),locationUpdatedAt:serverTime(),locationSource:source});
  });return {success:true};
});
