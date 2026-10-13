const {onCall,HttpsError,db,serverTime,requireAuth,id,text,fail,Timestamp,record} = require('../shared');
exports.manageMess = onCall('manageMess',async req=>{
  requireAuth(req,['admin']);const d=req.data,kind=d.kind;
  if(kind==='hall') {
    const operatorId=id(d.operatorId),operator=await require('../shared').getAuth().getUser(operatorId);
    if(operator.customClaims?.role!=='mess')fail('Select a provisioned mess operator.');
    const ref=db.doc(`messHalls/${d.id?id(d.id):db.collection('messHalls').doc().id}`);
    await ref.set({name:text(d.name,100),operatorId,open:d.open===true},{merge:true});return {id:ref.id};
  }
  const messId=id(d.messId);if(!(await db.doc(`messHalls/${messId}`).get()).exists)fail('Select an existing mess hall.');
  if(kind==='period') {
    if(!Number.isSafeInteger(d.opensAt)||!Number.isSafeInteger(d.closesAt)||d.closesAt<=d.opensAt||d.closesAt-d.opensAt>12*3600000)fail('Choose a meal window of up to 12 hours.');
    const ref=db.doc(`mealPeriods/${d.id?id(d.id):db.collection('mealPeriods').doc().id}`);
    await ref.set({messId,label:text(d.label,80),opensAt:Timestamp.fromMillis(d.opensAt),closesAt:Timestamp.fromMillis(d.closesAt),createdAt:serverTime()},{merge:true});return {id:ref.id};
  }
  if(kind==='entitlement') {
    const userId=id(d.userId),user=await require('../shared').getAuth().getUser(userId);
    if(user.customClaims?.role&&user.customClaims.role!=='student')fail('Meal entitlements are for students.');
    await db.doc(`mealEntitlements/${messId}_${userId}`).set({messId,userId,active:d.active===true,updatedAt:serverTime()},{merge:true});return {success:true};
  }
  fail('Choose a supported mess action.');
});
exports.getMealPass = onCall('getMealPass',async req=>{
  const {uid}=requireAuth(req,['student']);const messId=id(req.data.messId);
  const [hall,entitlement]=await Promise.all([db.doc(`messHalls/${messId}`).get(),db.doc(`mealEntitlements/${messId}_${uid}`).get()]);
  if(!hall.exists||!hall.data().open||!entitlement.exists||!entitlement.data().active)fail('No active meal entitlement for this mess.');
  // Purpose-bound opaque pass; never a Firebase sign-in credential.
  const {randomBytes}=require('node:crypto');const pass=randomBytes(24).toString('hex');
  await db.runTransaction(async tx=>{
    await require('../shared').requireActiveAccount(tx,uid);
    tx.set(db.doc(`mealPasses/${uid}`),{userId:uid,messId,token:pass,expiresAt:Timestamp.fromMillis(Date.now()+300000)});
  });
  return {payload:`civora-meal:${uid}:${pass}`,expiresAt:Date.now()+300000};
});
exports.checkInMeal = onCall('checkInMeal',async req=>{
  const {uid,role}=requireAuth(req,['mess','admin']);const periodId=id(req.data.periodId),userId=id(req.data.userId),token=text(req.data.token,128);
  return db.runTransaction(async tx=>{
    await require('../shared').requireActiveAccount(tx,userId);
    const period=await tx.get(db.doc(`mealPeriods/${periodId}`));if(!period.exists)fail('Meal period no longer exists.');const p=period.data();
    const [hall,entitlement,pass,previous]=await Promise.all([tx.get(db.doc(`messHalls/${p.messId}`)),tx.get(db.doc(`mealEntitlements/${p.messId}_${userId}`)),tx.get(db.doc(`mealPasses/${userId}`)),tx.get(db.doc(`mealCheckIns/${periodId}_${userId}`))]);
    if(!hall.exists||(role!=='admin'&&hall.data().operatorId!==uid))throw new HttpsError('permission-denied','This mess is not assigned to you.');
    if(previous.exists)fail('This meal has already been checked in.');
    if(!hall.data().open||Date.now()<p.opensAt.toMillis()||Date.now()>p.closesAt.toMillis())fail('This meal period is closed.');
    if(!entitlement.exists||!entitlement.data().active)fail('Student is not eligible for this meal.');
    if(!pass.exists||pass.data().messId!==p.messId||pass.data().token!==token||pass.data().expiresAt.toMillis()<Date.now())fail('Meal pass is invalid or expired.');
    tx.set(db.doc(`mealCheckIns/${periodId}_${userId}`),{periodId,messId:p.messId,userId,operatorId:hall.data().operatorId,label:p.label,checkedBy:uid,createdAt:serverTime()});
    tx.delete(pass.ref);record(tx,userId,{title:'Meal checked in',body:p.label,type:'NOTICE',status:'CHECKED_IN',destination:'FOOD',entityId:''});
    return {success:true};
  });
});
exports.getMessSummary = onCall('getMessSummary',async req=>{
  const {uid,role}=requireAuth(req,['mess','admin']);const periodId=id(req.data.periodId);
  const period=await db.doc(`mealPeriods/${periodId}`).get();if(!period.exists)fail('Select a meal period.');
  const hall=await db.doc(`messHalls/${period.data().messId}`).get();
  if(!hall.exists||(role!=='admin'&&hall.data().operatorId!==uid))throw new HttpsError('permission-denied','This mess is not assigned to you.');
  const result=await db.collection('mealCheckIns').where('periodId','==',periodId).count().get();
  return {periodId,count:result.data().count};
});
