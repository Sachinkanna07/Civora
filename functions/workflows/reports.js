const {onCall,HttpsError,db,serverTime,requireAuth,text,id,fail,record,reportTransitions,canTransition,getAuth} = require('../shared');
const {validateAttachment}=require('../attachments');
exports.submitReport = onCall('submitReport',async req => {
  const {uid}=requireAuth(req,['student']),d=req.data,requestId=id(d.requestId);
  const ref=db.doc(`campusReports/${uid}_${requestId}`),category=text(d.category,30);
  if(!['ELECTRICAL','WATER','CLEANLINESS','SAFETY','INFRASTRUCTURE','TRANSPORT','OTHER'].includes(category))fail('Choose an issue category.');
  const imagePath=await validateAttachment(d.imagePath||'',`reports/${uid}/${requestId}/`);
  const report={title:text(d.title,120),description:text(d.description,4000),category,location:text(d.location||'',200,false),imagePath,imageUrl:'',createdBy:uid,status:'SUBMITTED',priority:category==='SAFETY'?'high':'normal',assignedTo:'',assignedToName:'',resolutionNote:'',createdAt:serverTime(),updatedAt:serverTime()};
  await db.runTransaction(async tx=>{await require('../shared').requireActiveAccount(tx,uid);if((await tx.get(ref)).exists)return;
    tx.set(ref,report);tx.set(ref.collection('history').doc(),{actor:uid,status:'SUBMITTED',note:'Issue submitted',createdAt:serverTime()});
    record(tx,uid,{title:'Report submitted',body:report.title,type:'REPORT',status:'SUBMITTED',destination:'REPORT',entityId:ref.id});
  });return {reportId:ref.id};
});
exports.updateReport = onCall('updateReport',async req=>{
  const {uid}=requireAuth(req,['admin']),d=req.data,ref=db.doc(`campusReports/${id(d.reportId,257)}`),next=text(d.status,30);
  const note=text(d.resolutionNote||'',2000,false),assignedTo=text(d.assignedTo||'',128,false);
  if(next==='ASSIGNED') {
    if(!assignedTo)fail('Select a maintenance worker.');
    let assignee;try{assignee=await getAuth().getUser(id(assignedTo));}catch(_){fail('Worker account is unavailable.');}
    if(assignee.customClaims?.role!=='maintenance')fail('Assign a provisioned maintenance worker.');
  }
  await db.runTransaction(async tx=>{
    const snap=await tx.get(ref);if(!snap.exists)fail('Report no longer exists.');const r=snap.data();
    const staff=next==='ASSIGNED'?await tx.get(db.doc(`staffDirectory/${assignedTo}`)):null;
    if(staff&&(!staff.exists||staff.data().active!==true))fail('This worker is not active.');
    if(!canTransition(reportTransitions,r.status,next))fail('Status changed. Refresh this report.');
    if(['RESOLVED','REJECTED'].includes(next)&&!note)fail('Add a resolution or rejection note.');
    const assignment=next==='ASSIGNED'?{assignedTo,assignedToName:staff.data().name,assignedDepartment:staff.data().department,assignedTeam:staff.data().team}:{};
    tx.update(ref,{...assignment,status:next,resolutionNote:note||r.resolutionNote||'',updatedAt:serverTime(),...(next==='RESOLVED'?{resolvedAt:serverTime()}: {})});
    tx.set(ref.collection('history').doc(),{actor:uid,status:next,note:note||'Status updated',assignedTo:assignment.assignedTo||r.assignedTo||'',createdAt:serverTime()});
    record(tx,r.createdBy,{title:`Report ${next.toLowerCase().replaceAll('_',' ')}`,body:r.title,type:'REPORT',status:next,destination:'REPORT',entityId:ref.id});
    if(next==='ASSIGNED')record(tx,assignedTo,{title:'New maintenance assignment',body:r.title,type:'REPORT',status:next,destination:'REPORT',entityId:ref.id});
  });return {success:true};
});
exports.updateAssignedWork = onCall('updateAssignedWork',async req=>{
  const {uid}=requireAuth(req,['maintenance']),d=req.data,ref=db.doc(`campusReports/${id(d.reportId,257)}`),next=text(d.status,30),note=text(d.note||'',2000,false);
  if(!['ACCEPTED','IN_PROGRESS','REVIEW_REQUIRED','NOTE'].includes(next))throw new HttpsError('permission-denied','Only campus operations can approve final resolution.');
  const evidencePath=await validateAttachment(d.evidencePath||'',`reportEvidence/${ref.id}/${uid}/`);
  await db.runTransaction(async tx=>{
    const report=await tx.get(ref);if(!report.exists)fail('Report no longer exists.');const r=report.data();
    if(r.assignedTo!==uid)throw new HttpsError('permission-denied','This report is assigned to another worker.');
    if(['RESOLVED','REJECTED'].includes(r.status))fail('This report is closed.');
    if(next==='NOTE'&&!note)fail('Write a progress note.');
    if(next!=='NOTE'&&!canTransition(reportTransitions,r.status,next))fail('Status changed. Refresh your work queue.');
    if(next==='REVIEW_REQUIRED'&&!note)fail('Describe the completed work before requesting review.');
    tx.update(ref,{...(next==='NOTE'?{}:{status:next}),...(evidencePath?{completionImagePath:evidencePath}:{}),progressNote:note,updatedAt:serverTime()});
    tx.set(ref.collection('history').doc(),{actor:uid,status:next,note,evidencePath,createdAt:serverTime()});
    record(tx,r.createdBy,{title:next==='NOTE'?'Maintenance progress update':`Report ${next.toLowerCase().replaceAll('_',' ')}`,body:note||r.title,type:'REPORT',status:next==='NOTE'?r.status:next,destination:'REPORT',entityId:ref.id});
  });return {success:true};
});
