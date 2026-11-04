const {onCall,db,requireAuth,Timestamp}=require('../shared');
exports.getOperationsSummary = onCall('getOperationsSummary',async req=>{
  requireAuth(req,['admin']);
  const since=Timestamp.fromMillis(Date.now()-86400000);
  const active=['SUBMITTED','ASSIGNED','ACCEPTED','IN_PROGRESS','REVIEW_REQUIRED'];
  const queries={openReports:db.collection('campusReports').where('status','in',active),reviewRequired:db.collection('campusReports').where('status','==','REVIEW_REQUIRED'),activeTrips:db.collection('activeTrips').where('active','==',true),openOrders:db.collection('foodOrders').where('status','in',['PLACED','ACCEPTED','PREPARING','READY']),meals24h:db.collection('mealCheckIns').where('createdAt','>=',since),students:db.collection('users').where('role','==','student'),urgentReports:db.collection('campusReports').where('category','==','SAFETY').where('status','in',active)};
  const result={};await Promise.all(Object.entries(queries).map(async ([key,q])=>{result[key]=(await q.count().get()).data().count;}));
  return {...result,capturedAt:Date.now()};
});
