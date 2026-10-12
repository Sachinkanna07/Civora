const {onCall,HttpsError,db,serverTime,requireAuth,text,id,fail,record,Timestamp,getAuth,randomBytes,foodTransitions,reportTransitions,canTransition,hasCapacity,priceOrder} = require('../shared');
exports.manageCampusData = onCall('manageCampusData',async req => {
  const {uid}=requireAuth(req,['admin']); const d=req.data; const collection=d.collection; const entityId=d.id?id(d.id):db.collection('campusRecords').doc().id;
  let value;
  switch(collection) {
    case 'canteens': value={name:text(d.name,100),vendorId:id(d.vendorId),open:d.open===true}; break;
    case 'departments': value={name:text(d.name,100),code:text(d.code,80),years:Array.isArray(d.years)?d.years.slice(0,8).map(y=>text(y,20)):['1','2','3','4'],sections:Array.isArray(d.sections)?d.sections.slice(0,20).map(s=>text(s,20)):['A','B']};break;
    case 'academicCalendar': if(!/^\d{4}-\d{2}-\d{2}$/.test(d.date||''))fail('Choose a calendar date.'); value={date:d.date,title:text(d.title,120),department:text(d.department||'',80,false),holiday:d.holiday===true};break;
    case 'menuItems': if(!Number.isSafeInteger(d.pricePaise)||d.pricePaise<0||d.pricePaise>100000) fail('Invalid price.'); value={name:text(d.name,100),description:text(d.description||'',500,false),category:text(d.category||'Meals',50),canteenId:id(d.canteenId),pricePaise:d.pricePaise,available:d.available===true,imageUrl:''}; break;
    case 'buses': value={name:text(d.name,100),routeId:id(d.routeId),driverId:id(d.driverId)}; break;
    case 'busRoutes': if(!Array.isArray(d.stops)||d.stops.length>60) fail('Invalid stops.'); value={name:text(d.name,100),contactPhone:text(d.contactPhone||'',30,false),stops:d.stops.map(s=>{if(!s||!Number.isFinite(s.latitude)||Math.abs(s.latitude)>90||!Number.isFinite(s.longitude)||Math.abs(s.longitude)>180)fail('Invalid coordinates.');return {name:text(s.name,100),latitude:s.latitude,longitude:s.longitude};})};break;
    case 'clubs':value={name:text(d.name,100),description:text(d.description,4000),category:text(d.category,50),imageUrl:''};break;
    case 'campusLocations':if(!Number.isFinite(d.latitude)||Math.abs(d.latitude)>90||!Number.isFinite(d.longitude)||Math.abs(d.longitude)>180)fail('Invalid coordinates.');value={name:text(d.name,100),category:text(d.category,50),description:text(d.description||'',2000,false),latitude:d.latitude,longitude:d.longitude};break;
    case 'timetables': if(!Number.isInteger(d.dayOfWeek)||d.dayOfWeek<1||d.dayOfWeek>7||!Number.isInteger(d.startMinute)||!Number.isInteger(d.endMinute)||d.startMinute<0||d.endMinute>1440||d.endMinute<=d.startMinute)fail('Invalid schedule.');if(!/^\d{4}-\d{2}-\d{2}$/.test(d.effectiveFrom||'')||!/^\d{4}-\d{2}-\d{2}$/.test(d.effectiveTo||'')||d.effectiveTo<d.effectiveFrom)fail('Choose a valid semester date range.');value={courseId:id(d.courseId),courseName:text(d.courseName,100),faculty:text(d.faculty,100),dayOfWeek:d.dayOfWeek,startMinute:d.startMinute,endMinute:d.endMinute,room:text(d.room,100),department:text(d.department,80),year:text(d.year,20),section:text(d.section||'',20,false),effectiveFrom:d.effectiveFrom,effectiveTo:d.effectiveTo};break;
    case 'academicOverrides': {
      if(!/^\d{4}-\d{2}-\d{2}$/.test(d.date||''))fail('Choose an override date.');
      const kind=text(d.kind,20);if(!['CLASS','CANCEL'].includes(kind))fail('Choose class or cancellation.');
      const baseCourseId=text(d.baseCourseId||'',128,false);
      if(kind==='CANCEL'&&!baseCourseId)fail('Choose the recurring course to cancel.');
      if(kind==='CLASS'&&(!Number.isInteger(d.startMinute)||!Number.isInteger(d.endMinute)||d.startMinute<0||d.endMinute>1440||d.endMinute<=d.startMinute))fail('Choose valid class times.');
      value={date:d.date,kind,baseCourseId,courseId:kind==='CLASS'?id(d.courseId):baseCourseId,courseName:kind==='CLASS'?text(d.courseName,100):text(d.courseName||'Class cancelled',100,false),faculty:kind==='CLASS'?text(d.faculty,100):'',startMinute:kind==='CLASS'?d.startMinute:0,endMinute:kind==='CLASS'?d.endMinute:0,room:kind==='CLASS'?text(d.room,100):'',department:text(d.department,80),year:text(d.year,20),section:text(d.section||'',20,false),reason:text(d.reason,300)};break;
    }
    default: fail('Unsupported campus collection.');
  }
  if(collection==='timetables'||collection==='academicOverrides') {
    const ref=db.doc(`timetables/${entityId}`);
    if(collection==='academicOverrides') {
      const overrideRef=db.doc(`academicOverrides/${entityId}`);
      await db.runTransaction(async tx=>{
        const sameDate=await tx.get(db.collection('academicOverrides').where('date','==',value.date).limit(501));
        if(sameDate.size>500)fail('This date has too many overrides. Ask an operator to review the academic dataset.');
        if(value.kind==='CLASS')for(const row of sameDate.docs){if(row.id===entityId||row.data().kind!=='CLASS')continue;const other=row.data();const overlaps=other.startMinute<value.endMinute&&other.endMinute>value.startMinute;const cohort=other.department===value.department&&other.year===value.year&&(!other.section||!value.section||other.section===value.section);if(overlaps&&(cohort||String(other.room).toLowerCase()===value.room.toLowerCase()||String(other.faculty).toLowerCase()===value.faculty.toLowerCase()))fail(`Date override conflicts with ${other.courseName}.`);}
        tx.set(overrideRef,{...value,updatedAt:serverTime()});
        tx.set(db.collection('operationsAudit').doc(),{actor:uid,collection,entityId,action:'ACADEMIC_OVERRIDE_SAVED',createdAt:serverTime()});
      });
      return {success:true,id:entityId};
    }
    await db.runTransaction(async tx=>{
      const old=await tx.get(ref);
      const days=new Set([value.dayOfWeek,old.data()?.dayOfWeek].filter(Boolean));
      for(const day of days)await tx.get(db.doc(`scheduleLocks/day_${day}`));
      const schedules=await tx.get(db.collection('timetables').where('dayOfWeek','==',value.dayOfWeek).limit(1001));
      if(schedules.size>1000)fail('This day exceeds the scheduling safety limit. Ask an operator to review the timetable dataset.');
      for(const row of schedules.docs) {
        if(row.id===entityId)continue;
        const entry=row.data();const overlaps=entry.startMinute<value.endMinute&&entry.endMinute>value.startMinute;
        const cohort=entry.department===value.department&&entry.year===value.year&&(!entry.section||!value.section||entry.section===value.section);
        if(overlaps&&(cohort||String(entry.room||'').toLowerCase()===value.room.toLowerCase()||String(entry.faculty||'').toLowerCase()===value.faculty.toLowerCase()))
          fail(`Schedule conflicts with ${entry.courseName}. Check the class, room and faculty.`);
      }
      const revision=(old.data()?.revision||0)+1;
      tx.set(ref,{...value,revision,updatedAt:serverTime()});
      for(const day of days)tx.set(db.doc(`scheduleLocks/day_${day}`),{updatedAt:serverTime()});
      tx.set(db.collection('operationsAudit').doc(),{actor:uid,collection,entityId,action:old.exists?'UPDATED':'CREATED',createdAt:serverTime()});
    });
    return {success:true,id:entityId};
  }
  if(collection==='buses') {
    const driver=await getAuth().getUser(value.driverId);
    if(driver.customClaims?.role!=='driver')fail('Assign an existing driver and route.');
    await db.runTransaction(async tx=>{
      const [route,trip]=await Promise.all([tx.get(db.doc(`busRoutes/${value.routeId}`)),tx.get(db.doc(`activeTrips/${entityId}`))]);
      if(!route.exists)fail('Assign an existing driver and route.');
      if(trip.exists&&trip.data().active)fail('End the active trip before editing this assignment.');
      tx.set(db.doc(`buses/${entityId}`),value);
      tx.set(db.collection('operationsAudit').doc(),{actor:uid,collection,entityId,action:'ASSIGNMENT_UPDATED',createdAt:serverTime()});
    });
    return {success:true};
  }
  if(collection==='canteens') {const vendor=await getAuth().getUser(value.vendorId);if(vendor.customClaims?.role!=='vendor')fail('Assign an existing vendor account.');}
  if(collection==='menuItems'&&!(await db.doc(`canteens/${value.canteenId}`).get()).exists)fail('Create the canteen first.');
  const batch=db.batch();batch.set(db.doc(`${collection}/${entityId}`),value,{merge:true});
  batch.set(db.collection('operationsAudit').doc(),{actor:uid,collection,entityId,action:'SAVED',createdAt:serverTime()});
  await batch.commit();return {success:true,id:entityId};
});
