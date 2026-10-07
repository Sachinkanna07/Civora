const {onCall,HttpsError,db,serverTime,requireAuth,text,id,fail,record,Timestamp,getAuth,randomBytes,foodTransitions,reportTransitions,canTransition,hasCapacity,priceOrder} = require('../shared');
exports.invitePrivilegedUser = onCall(async req => {
  const {uid} = requireAuth(req, ['admin']); const email = text(req.data.email,254); const role = text(req.data.role,30);
  if (!['faculty','vendor','driver'].includes(role) || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) fail('Invalid invitation.');
  const name = text(req.data.name,100); const department = text(req.data.department || '',80,false);
  let user; try { user = await getAuth().getUserByEmail(email); } catch (e) { if (e.code !== 'auth/user-not-found') throw e; user = await getAuth().createUser({email,displayName:name}); }
  const previousRole = user.customClaims?.role;
  if (previousRole === 'admin') throw new HttpsError('permission-denied','Administrator accounts cannot be modified through invitations.');
  await getAuth().setCustomUserClaims(user.uid, {...user.customClaims,role});
  await db.doc(`users/${user.uid}`).set({uid:user.uid,name,email,role,department,year:'',section:'',routeId:'',profileImage:'',notificationsEnabled:true,createdAt:serverTime()}, {merge:true});
  const resetLink = await getAuth().generatePasswordResetLink(email);
  await db.collection('roleAudit').add({actor:uid,target:user.uid,role,createdAt:serverTime()});
  return {uid:user.uid,setupLink:resetLink};
});
