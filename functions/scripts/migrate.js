// Run with trusted ADC after backing up the database and pausing registration writes.
const {initializeApp}=require('firebase-admin/app');
const {getFirestore,FieldValue,Timestamp}=require('firebase-admin/firestore');
const {getAuth}=require('firebase-admin/auth');
initializeApp({projectId:process.env.GCLOUD_PROJECT||process.env.GOOGLE_CLOUD_PROJECT});
const db=getFirestore();
async function each(collection,fn){let cursor;while(true){let q=db.collection(collection).orderBy('__name__').limit(100);if(cursor)q=q.startAfter(cursor);const page=await q.get();if(page.empty)break;for(const d of page.docs)await fn(d);cursor=page.docs[page.docs.length-1];if(page.size<100)break;}}
(async()=>{
 await each('users',async d=>{const p=d.data(),auth=await getAuth().getUser(d.id);await d.ref.update({role:auth.customClaims?.role||'student',section:p.section||'',routeId:p.routeId||'',notificationsEnabled:p.notificationsEnabled!==false,createdAt:p.createdAt instanceof Timestamp?p.createdAt:Timestamp.fromMillis(Number(p.createdAt)||Date.now())});});
 await each('announcements',async d=>{const a=d.data();await d.ref.update({audience:a.audience==='all'?'students':a.audience||'students',department:a.department||'',createdAt:a.createdAt instanceof Timestamp?a.createdAt:Timestamp.fromMillis(Number(a.createdAt)||Date.now())});});
 await each('events',async d=>{const regs=await db.collection('eventRegistrations').where('eventId','==',d.id).count().get();await d.ref.update({registeredCount:regs.data().count});});
 await each('eventRegistrations',async d=>{const r=d.data(),event=await db.doc(`events/${r.eventId}`).get();if(!event.exists)throw Error(`Orphan registration ${d.id}; review manually`);await d.ref.update({organizerId:event.data().createdBy,token:r.token||require('node:crypto').randomBytes(24).toString('hex'),attended:r.attended===true,createdAt:r.createdAt||FieldValue.serverTimestamp()});});
 console.log('Profile, announcement and registration migration complete.');
})().catch(e=>{console.error(e.message);process.exitCode=1});
