const config=require('./campus-config.json');
function deletionEnabled(emulator=process.env.FUNCTIONS_EMULATOR==='true') {
  return emulator||(config.retention.approved===true&&config.retention.productionDeletionEnabled===true);
}
module.exports={deletionEnabled};
