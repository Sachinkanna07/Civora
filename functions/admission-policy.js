const config=require('./campus-config.json');
const canonicalEmail=value=>typeof value==='string'?value.trim().toLowerCase():'';
function institutionalEmail(value) {
  const parts=canonicalEmail(value).split('@');
  return parts.length===2&&!!parts[0]&&parts[1]===config.emailDomain;
}
function verifiedIdentity(user,token) {
  return !user.disabled&&user.emailVerified===true&&token.email_verified===true&&!!canonicalEmail(user.email)&&canonicalEmail(token.email)===canonicalEmail(user.email);
}
function eligible(user,token,profile) {
  return verifiedIdentity(user,token)&&(institutionalEmail(user.email)||(profile?.admissionException===true&&canonicalEmail(profile.admissionEmail)===canonicalEmail(user.email)));
}
function hasCampusAccess(user,token,profile) {
  return profile?.accessDisabled!==true&&profile?.role===(user.customClaims?.role||'student')&&eligible(user,token,profile)&&profile?.membershipGranted===true&&canonicalEmail(profile.membershipEmail)===canonicalEmail(user.email);
}
module.exports={canonicalEmail,institutionalEmail,verifiedIdentity,eligible,hasCampusAccess};
