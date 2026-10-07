const {onCall,HttpsError,db,serverTime,requireAuth,text,id,fail,record,Timestamp,getAuth,randomBytes,foodTransitions,reportTransitions,canTransition,hasCapacity,priceOrder} = require('../shared');
exports.manageCampusData = onCall(async req => {
  requireAuth(req,['admin']); const d=req.data; const collection=d.collection; const entityId=id(d.id);
  let value;
  switch(collection) {
    case 'canteens': value={name:text(d.name,100),vendorId:id(d.vendorId),open:d.open===true}; break;
    case 'menuItems': if(!Number.isSafeInteger(d.pricePaise)||d.pricePaise<0||d.pricePaise>100000) fail('Invalid price.'); value={name:text(d.name,100),canteenId:id(d.canteenId),pricePaise:d.pricePaise,available:d.available===true,imageUrl:''}; break;
    case 'buses': value={name:text(d.name,100),routeId:id(d.routeId),driverId:id(d.driverId)}; break;
    case 'busRoutes': if(!Array.isArray(d.stops)||d.stops.length>60) fail('Invalid stops.'); value={name:text(d.name,100),stops:d.stops.map(s=>{if(!Number.isFinite(s.latitude)||Math.abs(s.latitude)>90||!Number.isFinite(s.longitude)||Math.abs(s.longitude)>180)fail('Invalid coordinates.');return {name:text(s.name,100),latitude:s.latitude,longitude:s.longitude};})};break;
    case 'clubs':value={name:text(d.name,100),description:text(d.description,4000),category:text(d.category,50),imageUrl:''};break;
    case 'campusLocations':if(!Number.isFinite(d.latitude)||Math.abs(d.latitude)>90||!Number.isFinite(d.longitude)||Math.abs(d.longitude)>180)fail('Invalid coordinates.');value={name:text(d.name,100),category:text(d.category,50),description:text(d.description||'',2000,false),latitude:d.latitude,longitude:d.longitude};break;
    case 'timetables': if(!Number.isInteger(d.dayOfWeek)||d.dayOfWeek<1||d.dayOfWeek>7||!Number.isInteger(d.startMinute)||!Number.isInteger(d.endMinute)||d.startMinute<0||d.endMinute>1440||d.endMinute<=d.startMinute)fail('Invalid schedule.');value={courseId:id(d.courseId),courseName:text(d.courseName,100),faculty:text(d.faculty,100),dayOfWeek:d.dayOfWeek,startMinute:d.startMinute,endMinute:d.endMinute,room:text(d.room,100),department:text(d.department,80),year:text(d.year,20),section:text(d.section||'',20,false)};break;
    default: fail('Unsupported campus collection.');
  }
  if(collection==='buses') {
    const [driver,route,trip]=await Promise.all([getAuth().getUser(value.driverId),db.doc(`busRoutes/${value.routeId}`).get(),db.doc(`activeTrips/${entityId}`).get()]);
    if(driver.customClaims?.role!=='driver'||!route.exists)fail('Assign an existing driver and route.');
    if(trip.exists&&trip.data().active)fail('End the active trip before editing this assignment.');
  }
  if(collection==='canteens') {const vendor=await getAuth().getUser(value.vendorId);if(vendor.customClaims?.role!=='vendor')fail('Assign an existing vendor account.');}
  if(collection==='menuItems'&&!(await db.doc(`canteens/${value.canteenId}`).get()).exists)fail('Create the canteen first.');
  await db.doc(`${collection}/${entityId}`).set(value);return {success:true};
});
