const {onCall,HttpsError,db,serverTime,requireAuth,id,text,fail} = require('../shared');
exports.setCanteenOperations = onCall('setCanteenOperations',async req => {
  const {uid,role}=requireAuth(req,['vendor','admin']);
  const ref=db.doc(`canteens/${id(req.data.canteenId)}`);
  const open=req.data.open, paused=req.data.paused;
  const preparationMinutes=req.data.preparationMinutes;
  if(typeof open!=='boolean'||typeof paused!=='boolean'||(preparationMinutes!==null&&(!Number.isInteger(preparationMinutes)||preparationMinutes<1||preparationMinutes>180)))
    throw new HttpsError('invalid-argument','Check opening status and preparation time (1–180 minutes).');
  await db.runTransaction(async tx=>{
    const snap=await tx.get(ref);if(!snap.exists)fail('Canteen no longer exists.');
    if(role!=='admin'&&snap.data().vendorId!==uid)throw new HttpsError('permission-denied','This kitchen is not assigned to you.');
    tx.update(ref,{open,paused,preparationMinutes,updatedAt:serverTime()});
    tx.set(db.collection('operationsAudit').doc(),{actor:uid,type:'CANTEEN',entityId:ref.id,open,paused,preparationMinutes,createdAt:serverTime()});
  });return {success:true};
});
exports.saveMenuItem = onCall('saveMenuItem',async req=>{
  const {uid,role}=requireAuth(req,['vendor','admin']);const d=req.data;
  const ref=db.doc(`menuItems/${d.id?id(d.id):db.collection('menuItems').doc().id}`),canteenId=id(d.canteenId);
  if(!Number.isSafeInteger(d.pricePaise)||d.pricePaise<0||d.pricePaise>100000)fail('Enter a price between INR 0 and INR 1000.');
  const value={name:text(d.name,100),description:text(d.description||'',500,false),category:text(d.category||'Meals',50),canteenId,pricePaise:d.pricePaise,available:d.available===true,imageUrl:''};
  await db.runTransaction(async tx=>{const [canteen,old]=await Promise.all([tx.get(db.doc(`canteens/${canteenId}`)),tx.get(ref)]);
    if(!canteen.exists||(role!=='admin'&&canteen.data().vendorId!==uid))throw new HttpsError('permission-denied','Menu is not assigned to you.');
    if(old.exists&&old.data().canteenId!==canteenId)fail('A menu item cannot move between kitchens.');tx.set(ref,value,{merge:true});
  });return {id:ref.id};
});
