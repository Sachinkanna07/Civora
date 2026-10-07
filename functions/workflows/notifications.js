const {onCall,HttpsError,db,serverTime,requireAuth,text,id,fail,record,Timestamp,getAuth,randomBytes,foodTransitions,reportTransitions,canTransition,hasCapacity,priceOrder} = require('../shared');
const {onDocumentCreated} = require('firebase-functions/v2/firestore');
const {getMessaging} = require('firebase-admin/messaging');
// Durable inbox rows are written regardless of FCM/device permission.
exports.announcementInbox = onDocumentCreated('announcements/{announcementId}', async event => {
  const a=event.data.data(); if(a.audience !== 'students' || (a.expiresAt && a.expiresAt.toMillis()<Date.now()))return;
  let cursor;
  while(true) {
    let q=db.collection('users').where('role','==','student').orderBy('__name__').limit(100);
    if(a.department)q=q.where('department','==',a.department);
    if(cursor)q=q.startAfter(cursor);
    const users=await q.get();if(users.empty)break;
    await Promise.all(users.docs.map(user=>db.runTransaction(async tx=>{
      const ref=db.doc(`notifications/notice_${event.params.announcementId}_${user.id}`);const prior=await tx.get(ref);if(prior.exists)return;
      const base={userId:user.id,title:a.title,body:a.body,type:'NOTICE',status:a.priority.toUpperCase(),destination:'ANNOUNCEMENT',entityId:event.params.announcementId,createdAt:serverTime()};
      tx.set(ref,{...base,category:a.priority==='urgent'?'URGENT':'RELEVANT',read:false});
      tx.set(db.doc(`activities/notice_${event.params.announcementId}_${user.id}`),base);
    })));
    cursor=users.docs[users.docs.length-1];if(users.size<100)break;
  }
});
exports.sendInboxPush = onDocumentCreated('notifications/{notificationId}', async event=>{
  const n=event.data.data();const profile=await db.doc(`users/${n.userId}`).get();
  if(!profile.exists||profile.data().notificationsEnabled===false)return;
  const devices=await db.collection(`users/${n.userId}/devices`).limit(10).get();if(devices.empty)return;
  const response=await getMessaging().sendEachForMulticast({tokens:devices.docs.map(d=>d.data().token),data:{userId:n.userId,title:n.title.slice(0,200),body:n.body.slice(0,1000),notificationId:event.params.notificationId}});
  await Promise.all(response.responses.map((result,index)=>['messaging/registration-token-not-registered','messaging/invalid-registration-token'].includes(result.error?.code)?devices.docs[index].ref.delete():Promise.resolve()));
});

const {onDocumentUpdated} = require('firebase-functions/v2/firestore');
exports.registeredEventUpdate = onDocumentUpdated('events/{eventId}',async event=>{
  const before=event.data.before.data(),after=event.data.after.data();
  if(before.title===after.title&&before.venue===after.venue&&before.startTime?.toMillis()===after.startTime?.toMillis()&&before.endTime?.toMillis()===after.endTime?.toMillis())return;
  let cursor;
  while(true){let query=db.collection('eventRegistrations').where('eventId','==',event.params.eventId).orderBy('__name__').limit(100);if(cursor)query=query.startAfter(cursor);const regs=await query.get();if(regs.empty)break;
    await Promise.all(regs.docs.map(async reg=>{const ref=db.doc(`notifications/event_${event.id.replaceAll('/','_')}_${reg.data().userId}`);await db.runTransaction(async tx=>{if((await tx.get(ref)).exists)return;tx.set(ref,{userId:reg.data().userId,title:'Registered event updated',body:after.title,type:'NOTICE',status:'UPDATED',destination:'EVENT',entityId:event.params.eventId,createdAt:serverTime(),category:'RELEVANT',read:false});});}));
    cursor=regs.docs[regs.docs.length-1];if(regs.size<100)break;
  }
});
