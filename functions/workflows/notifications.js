const {onCall,HttpsError,db,serverTime,requireAuth,text,id,fail,record,Timestamp,getAuth,randomBytes,foodTransitions,reportTransitions,canTransition,hasCapacity,priceOrder} = require('../shared');
const {onDocumentCreated,onDocumentUpdated,onDocumentWritten} = require('firebase-functions/v2/firestore');
const {getMessaging} = require('firebase-admin/messaging');
const MAX_FANOUT = 5000;
async function recordFanoutLimit(kind,entityId,delivered) {
  await db.collection('operationsAudit').add({kind:'NOTIFICATION_FANOUT_LIMIT',source:kind,entityId,delivered,limit:MAX_FANOUT,createdAt:serverTime()});
}
// Durable inbox rows are written regardless of FCM/device permission.
exports.announcementInbox = onDocumentCreated('announcements/{announcementId}', async event => {
  const a=event.data.data(); if(a.audience !== 'students' || (a.expiresAt && a.expiresAt.toMillis()<Date.now()))return;
  let cursor,delivered=0;
  while(delivered<MAX_FANOUT) {
    let q=db.collection('users').where('role','==','student').orderBy('__name__').limit(Math.min(100,MAX_FANOUT-delivered));
    if(a.department)q=q.where('department','==',a.department);
    if(cursor)q=q.startAfter(cursor);
    const users=await q.get();if(users.empty)break;
    await Promise.all(users.docs.map(user=>db.runTransaction(async tx=>{
      const ref=db.doc(`notifications/notice_${event.params.announcementId}_${user.id}`);const prior=await tx.get(ref);if(prior.exists)return;
      const base={userId:user.id,title:a.title,body:a.body,type:'NOTICE',status:a.priority.toUpperCase(),destination:'ANNOUNCEMENT',entityId:event.params.announcementId,createdAt:serverTime()};
      tx.set(ref,{...base,category:a.priority==='urgent'?'URGENT':'RELEVANT',read:false});
      tx.set(db.doc(`activities/notice_${event.params.announcementId}_${user.id}`),base);
    })));
    delivered+=users.size;cursor=users.docs[users.docs.length-1];if(users.size<100)break;
  }
  if(delivered===MAX_FANOUT)await recordFanoutLimit('announcement',event.params.announcementId,delivered);
});
exports.sendInboxPush = onDocumentCreated('notifications/{notificationId}', async event=>{
  const n=event.data.data();const profile=await db.doc(`users/${n.userId}`).get();
  if(!profile.exists||profile.data().notificationsEnabled===false||profile.data().accessDisabled===true||profile.data().membershipGranted!==true)return;
  const recipient=await getAuth().getUser(n.userId).catch(()=>null);
  if(!recipient||!require('../admission-policy').hasCampusAccess(recipient,{email:recipient.email,email_verified:recipient.emailVerified},profile.data()))return;
  const devices=await db.collection(`users/${n.userId}/devices`).limit(10).get();if(devices.empty)return;
  const response=await getMessaging().sendEachForMulticast({tokens:devices.docs.map(d=>d.data().token),data:{userId:n.userId,title:n.title.slice(0,200),body:n.body.slice(0,1000),notificationId:event.params.notificationId,destination:n.destination||'',entityId:n.entityId||''}});
  await Promise.all(response.responses.map((result,index)=>['messaging/registration-token-not-registered','messaging/invalid-registration-token'].includes(result.error?.code)?devices.docs[index].ref.delete():Promise.resolve()));
});

exports.registeredEventUpdate = onDocumentUpdated('events/{eventId}',async event=>{
  const before=event.data.before.data(),after=event.data.after.data();
  if(before.cancelled===after.cancelled&&before.title===after.title&&before.venue===after.venue&&before.startTime?.toMillis()===after.startTime?.toMillis()&&before.endTime?.toMillis()===after.endTime?.toMillis())return;
  let cursor,delivered=0;
  while(delivered<MAX_FANOUT){let query=db.collection('eventRegistrations').where('eventId','==',event.params.eventId).orderBy('__name__').limit(Math.min(100,MAX_FANOUT-delivered));if(cursor)query=query.startAfter(cursor);const regs=await query.get();if(regs.empty)break;
    await Promise.all(regs.docs.map(async reg=>{const ref=db.doc(`notifications/event_${event.id.replaceAll('/','_')}_${reg.data().userId}`);await db.runTransaction(async tx=>{if((await tx.get(ref)).exists)return;tx.set(ref,{userId:reg.data().userId,title:after.cancelled?'Registered event cancelled':'Registered event updated',body:after.title,type:'NOTICE',status:after.cancelled?'CANCELLED':'UPDATED',destination:'EVENT',entityId:event.params.eventId,createdAt:serverTime(),category:'RELEVANT',read:false});});}));
    delivered+=regs.size;cursor=regs.docs[regs.docs.length-1];if(regs.size<100)break;
  }
  if(delivered===MAX_FANOUT)await recordFanoutLimit('event',event.params.eventId,delivered);
});
exports.transportUpdateInbox = onDocumentCreated('transportUpdates/{updateId}',async event=>{
  const update=event.data.data();let cursor,delivered=0;
  while(delivered<MAX_FANOUT){let query=db.collection('users').where('role','==','student').where('routeId','==',update.routeId).orderBy('__name__').limit(Math.min(100,MAX_FANOUT-delivered));if(cursor)query=query.startAfter(cursor);
    const users=await query.get();if(users.empty)break;
    await Promise.all(users.docs.map(user=>db.runTransaction(async tx=>{
      const ref=db.doc(`notifications/transport_${event.params.updateId}_${user.id}`);if((await tx.get(ref)).exists)return;
      tx.set(ref,{userId:user.id,title:`Transport ${update.kind.toLowerCase()}`,body:update.note||`${update.busName}${update.delayMinutes?` · ${update.delayMinutes} minute delay`:''}`,type:'NOTICE',status:update.kind,destination:'TRANSPORT',entityId:update.busId,createdAt:serverTime(),category:update.kind==='BREAKDOWN'?'URGENT':'RELEVANT',read:false});
    })));
    delivered+=users.size;cursor=users.docs[users.docs.length-1];if(users.size<100)break;
  }
  if(delivered===MAX_FANOUT)await recordFanoutLimit('transport',event.params.updateId,delivered);
});

exports.scheduleUpdateInbox = onDocumentUpdated('timetables/{scheduleId}',async event=>{
  const before=event.data.before.data(),after=event.data.after.data();
  if(['courseName','faculty','dayOfWeek','startMinute','endMinute','room','department','year','section'].every(key=>before[key]===after[key]))return;
  const notified=new Set();let capped=false;
  for(const cohort of [before,after]) {
    let cursor;
    for(;notified.size<MAX_FANOUT;) {
      let query=db.collection('users').where('role','==','student').where('department','==',cohort.department).where('year','==',cohort.year).orderBy('__name__').limit(Math.min(100,MAX_FANOUT-notified.size));
      if(cohort.section)query=query.where('section','==',cohort.section);
      if(cursor)query=query.startAfter(cursor);
      const users=await query.get();if(users.empty)break;
      for(const user of users.docs) {
        if(notified.has(user.id))continue;notified.add(user.id);
        const ref=db.doc(`notifications/schedule_${event.id.replaceAll('/','_')}_${user.id}`);
        await db.runTransaction(async tx=>{if((await tx.get(ref)).exists)return;
          tx.set(ref,{userId:user.id,title:'Timetable updated',body:`${after.courseName}: check your latest timetable.`,type:'NOTICE',status:'UPDATED',destination:'TIMETABLE',entityId:'',createdAt:serverTime(),category:'RELEVANT',read:false});
        });
      }
      cursor=users.docs.at(-1);if(users.size<100)break;
    }
    if(notified.size===MAX_FANOUT){capped=true;break;}
  }
  if(capped)await recordFanoutLimit('timetable',event.params.scheduleId,notified.size);
});

// Date-specific cancellations and replacement classes are authoritative and
// generate durable inbox rows for the affected cohort on every revision.
exports.academicOverrideInbox = onDocumentWritten('academicOverrides/{overrideId}',async event=>{
  const after=event.data.after;if(!after.exists)return;
  const override=after.data();
  const revision=after.updateTime.toMillis();let cursor,delivered=0;
  while(delivered<MAX_FANOUT){
    let query=db.collection('users').where('role','==','student').where('department','==',override.department).where('year','==',override.year).orderBy('__name__').limit(Math.min(100,MAX_FANOUT-delivered));
    if(override.section)query=query.where('section','==',override.section);
    if(cursor)query=query.startAfter(cursor);
    const users=await query.get();if(users.empty)break;
    await Promise.all(users.docs.map(user=>{
      const ref=db.doc(`notifications/override_${event.params.overrideId}_${revision}_${user.id}`);
      return ref.set({userId:user.id,title:override.kind==='CANCEL'?'Class cancelled':'Class schedule updated',body:`${override.date}: ${override.courseName||override.reason||'Check your timetable'}`,type:'NOTICE',status:override.kind,destination:'TIMETABLE',entityId:event.params.overrideId,createdAt:serverTime(),category:'RELEVANT',read:false});
    }));
    delivered+=users.size;cursor=users.docs.at(-1);if(users.size<100)break;
  }
  if(delivered===MAX_FANOUT)await recordFanoutLimit('academicOverride',event.params.overrideId,delivered);
});
