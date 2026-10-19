package dev.aragonite.powersync.client;
import android.content.Context;import android.os.Environment;import android.util.AtomicFile;import android.util.Base64;import org.json.*;import java.io.*;import java.nio.file.*;import java.nio.charset.StandardCharsets;
final class TransitionController {
 final NativeBridge nativeIO;final PrivateState state;final Context context;
 TransitionController(Context c){context=c.getApplicationContext();nativeIO=new NativeBridge(context);state=new PrivateState(context);}
 private File marker(){return new File(Environment.getExternalStorageDirectory(),"onyx_test_server");}
 private void storage()throws IOException {if(!Environment.isExternalStorageManager())throw new UserError(R.string.error_grant_all_files_access_before_configuring_or);}
 interface Progress { void update(int message); }
 Progress progress = message -> {};
 private void phase(JSONObject journal,String phase)throws Exception {
  int message = switch(phase) {
   case "prepared" -> R.string.progress_saving;
   case "pause_requested", "paused" -> R.string.progress_pausing;
   case "login_requested", "authenticated" -> R.string.progress_signing_in;
   case "enable_requested" -> R.string.progress_enabling;
   case "restore_requested" -> R.string.progress_restoring;
   default -> R.string.progress_connecting;
  };
  progress.update(message);journal.put("phase",phase).put("updatedAt",System.currentTimeMillis());state.write("transition",journal);}
 JSONObject localStatus()throws Exception {
  JSONObject j=state.read("transition"), p=state.read("profile"), update=state.read("endpoint-update");
  String phase=j==null?"not_configured":j.optString("phase");
  return new JSONObject().put("phase",phase).put("hasBaseline",state.read("baseline")!=null)
   .put("allFilesAccess",Environment.isExternalStorageManager()).put("applicationVerified",false)
   .put("serverURL",p==null?"":p.optString("serverURL"))
   .put("enrollmentReady",p!=null&&(p.optBoolean("enrollmentReady")||j==null))
   .put("endpointInterrupted",update!=null&&!java.util.Set.of("applied","rolled_back").contains(update.optString("phase"))&&!"restored".equals(phase));
 }
 JSONObject status()throws Exception {
  JSONObject s=localStatus();nativeIO.requireKsyncAvailable();
  JSONObject c=nativeIO.read(NativeBridge.CLUSTER), a=nativeIO.account(), p=state.read("profile"), installed=state.read("installed-cluster");
  String selected=c.getString("curClusterId");JSONObject entry=c.getJSONObject("clusterMap").optJSONObject(selected);
  return s.put("available",true).put("clusterId",selected).put("accountLoggedIn",a.optBoolean("loggedIn"))
   .put("flags",nativeIO.flags()).put("identityMatches",p!=null&&a.optBoolean("loggedIn")&&p.getString("nativeUid").equals(a.optString("uid"))&&selected.equals(a.optString("clusterId"))&&p.getJSONObject("cluster").getString("clusterId").equals(selected))
   .put("enrolledClusterSelected",p!=null&&p.getJSONObject("cluster").getString("clusterId").equals(selected))
   .put("configurationMatches",entry!=null&&installed!=null&&NativeBridge.canonical(entry).equals(NativeBridge.canonical(installed)));
 }
 void enableSync()throws Exception {
  storage();ConnectionState current=new ConnectionState(status());
  if(current.screen!=ConnectionState.Screen.CONNECTED)throw new UserError(R.string.error_connection_changed);
  progress.update(R.string.progress_enabling);enableFlags();
 }
 private void enableFlags()throws Exception {
  nativeIO.flags(new JSONObject().put("notes","true").put("reader","true"));
  JSONObject actual=nativeIO.flags();
  if(!"true".equals(actual.optString("notes"))||!"true".equals(actual.optString("reader")))throw new UserError(R.string.error_sync_readback);
 }
 JSONObject enroll(String base,String code)throws Exception {nativeIO.requireKsyncAvailable();JSONObject journal=state.read("transition");if(journal!=null&&!java.util.Set.of("restored").contains(journal.optString("phase")))throw new UserError(R.string.error_return_to_the_previous_server_before_enrolling);String install=state.installationId();String id=nativeIO.read(NativeBridge.SYSTEM).optString("deviceUniqueId","");if(id.isEmpty())id=android.provider.Settings.Secure.getString(context.getContentResolver(),android.provider.Settings.Secure.ANDROID_ID);if(id==null||id.isEmpty())throw new UserError(R.string.error_native_device_identity_unavailable);JSONObject originalAccount=nativeIO.account();String owner=originalAccount.optBoolean("loggedIn")?originalAccount.getString("uid"):"";JSONObject p=EndpointProfile.validate(base,ServerClient.redeem(base,code,install,id,owner));if(!owner.isEmpty()&&!owner.equals(p.getString("nativeUid")))throw new UserError(R.string.error_server_did_not_preserve_your_boox_library);p.put("enrollmentAccount",originalAccount).put("enrollmentReady",true);state.write("profile",p);return new JSONObject().put("serverURL",base).put("libraryId",p.getString("libraryId")).put("ready",true);}
 void connect()throws Exception {storage();nativeIO.requireKsyncAvailable();JSONObject profile=state.read("profile");if(profile==null)throw new UserError(R.string.error_enroll_this_device_first);JSONObject journal=state.read("transition");if(journal!=null&&!java.util.Set.of("restored").contains(journal.optString("phase")))throw new UserError(R.string.error_a_transition_already_exists_use_return_to);JSONObject enrollmentAccount=profile.optJSONObject("enrollmentAccount");if(enrollmentAccount==null)throw new UserError(R.string.error_enroll_again_to_preserve_your_existing_boox);if(!NativeBridge.canonical(enrollmentAccount).equals(NativeBridge.canonical(nativeIO.account())))throw new UserError(R.string.error_your_boox_account_changed_after_enrollment_enroll);JSONObject current=nativeIO.read(NativeBridge.CLUSTER);String id=profile.getJSONObject("cluster").getString("clusterId");if(current.getJSONObject("clusterMap").has(id))throw new UserError(R.string.error_target_cluster_already_exists_inspect_instead_of);byte[] markerBytes=marker().exists()?Files.readAllBytes(marker().toPath()):null;if(markerBytes!=null&&markerBytes.length>1048576)throw new UserError(R.string.error_native_server_marker_exceeds_bound);JSONArray markers=markerBytes==null||markerBytes.length==0?new JSONArray():new JSONArray(new String(markerBytes,StandardCharsets.UTF_8));for(int i=0;i<markers.length();i++)if(id.equals(markers.getJSONObject(i).optString("serverId")))throw new UserError(R.string.error_marker_already_contains_this_cluster);progress.update(R.string.progress_saving);JSONArray catalog=nativeIO.catalog();JSONObject before=new JSONObject().put("cluster",current).put("flags",nativeIO.flags()).put("account",nativeIO.account()).put("accountCache",new NativeAccountBackup(nativeIO).capture(profile.getString("nativeUid"))).put("fingerprint",nativeIO.fingerprint()).put("catalog",catalog).put("catalogFiles",nativeIO.catalogFiles(catalog)).put("marker",markerBytes==null?JSONObject.NULL:Base64.encodeToString(markerBytes,Base64.NO_WRAP));state.write("baseline",before);journal=new JSONObject().put("phase","prepared").put("clusterId",id).put("nativeUid",profile.getString("nativeUid"));state.write("transition",journal);profile.put("enrollmentReady",false);state.write("profile",profile);
  try{phase(journal,"pause_requested");nativeIO.flags(new JSONObject().put("notes","false").put("reader","false"));phase(journal,"paused");JSONObject map=nativeIO.read(NativeBridge.CLUSTER);map.getJSONObject("clusterMap").put(id,profile.getJSONObject("cluster"));if(map.has("clusterMapSize"))map.put("clusterMapSize",map.getJSONObject("clusterMap").length());state.write("installed-cluster",profile.getJSONObject("cluster"));phase(journal,"configure_requested");nativeIO.write(NativeBridge.CLUSTER,map);markers.put(EndpointProfile.marker(profile));byte[] installedBytes=markers.toString().getBytes(StandardCharsets.UTF_8);state.write("installed-marker",new JSONObject().put("sha256",NativeBridge.hex(java.security.MessageDigest.getInstance("SHA-256").digest(installedBytes))));writeMarker(installedBytes);phase(journal,"configured");phase(journal,"refresh_requested");nativeIO.updateClusters();JSONObject refreshedDefinition=nativeIO.read(NativeBridge.CLUSTER).getJSONObject("clusterMap").getJSONObject(id);if(!NativeBridge.canonical(refreshedDefinition.getJSONObject("clusterHost")).equals(NativeBridge.canonical(profile.getJSONObject("cluster").getJSONObject("clusterHost"))))throw new UserError(R.string.error_native_refresh_replaced_the_enrolled_endpoints);state.write("installed-cluster",refreshedDefinition);phase(journal,"refreshed");phase(journal,"switch_requested");nativeIO.switchCluster(id);phase(journal,"switched");finishConnection(before,profile,journal);}catch(Exception e){journal.put("failure","Transition interrupted; baseline preserved");phase(journal,"failed");throw e;}}
 // Continue an interrupted switch from its original baseline; never recapture it.
 void resumeConnection()throws Exception {
  storage();nativeIO.requireKsyncAvailable();
  JSONObject status=status();
  if(!ConnectionRecovery.canResume(status))throw new UserError(R.string.error_connection_changed);
  JSONObject before=state.read("baseline"),profile=state.read("profile"),journal=state.read("transition");
  if(before==null||profile==null||journal==null)throw new UserError(R.string.error_no_saved_previous_configuration);
  String id=profile.getJSONObject("cluster").getString("clusterId");
  if(!id.equals(journal.optString("clusterId"))||!profile.getString("nativeUid").equals(journal.optString("nativeUid")))
   throw new UserError(R.string.error_native_account_mapping_differs);
  JSONObject installedMarker=state.read("installed-marker");
  byte[] bytes=marker().isFile()?Files.readAllBytes(marker().toPath()):null;
  if(installedMarker==null||bytes==null||!installedMarker.getString("sha256").equals(NativeBridge.hex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes))))
   throw new UserError(R.string.error_native_marker_changed_outside_powersync_preserve_it);
  try {
   nativeIO.flags(new JSONObject().put("notes","false").put("reader","false"));
   finishConnection(before,profile,journal);
  }catch(Exception e){journal.put("failure","Transition interrupted; baseline preserved");phase(journal,"failed");throw e;}
 }
 private void finishConnection(JSONObject before,JSONObject profile,JSONObject journal)throws Exception {
  phase(journal,"catalog_restore_requested");
  nativeIO.repairMissingCatalog(before.getJSONArray("catalog"));
  if(!before.getString("fingerprint").equals(nativeIO.fingerprint()))
   throw new UserError(R.string.error_native_library_changed_during_transition_synchronization_remains);
  JSONObject account=nativeIO.account();
  if(!account.optBoolean("loggedIn")) {
   new NativeAccountBackup(nativeIO).verifyBeforeLogin(before.getJSONArray("accountCache"),profile.getString("nativeUid"));
   phase(journal,"login_requested");
   nativeIO.login(profile.getJSONObject("account").getString("account"),profile.getJSONObject("account").getString("code"),false);
   account=nativeIO.account();
  }
  if(!account.optBoolean("loggedIn")||!profile.getString("nativeUid").equals(account.optString("uid"))||!profile.getJSONObject("cluster").getString("clusterId").equals(account.optString("clusterId")))
   throw new UserError(R.string.error_native_account_mapping_differs);
  nativeIO.repairMissingCatalog(before.getJSONArray("catalog"));
  if(!before.getString("fingerprint").equals(nativeIO.fingerprint()))
   throw new UserError(R.string.error_native_library_changed_during_sign_in_synchronization);
  int missing=CatalogRecovery.newlyUnavailable(before.optJSONObject("catalogFiles"),nativeIO.catalogFiles(before.getJSONArray("catalog")));
  journal.put("previouslyLocalBooksUnavailable",missing);
  if(missing>0)android.util.Log.w("PowerSyncCompatibility","Previously local book files unavailable: "+missing+"; catalog metadata preserved");
  phase(journal,"authenticated");phase(journal,"enable_requested");enableFlags();phase(journal,"active");
 }
 private void writeMarker(byte[] raw)throws Exception {AtomicFile f=new AtomicFile(marker());FileOutputStream out=f.startWrite();try{out.write(raw);out.getFD().sync();f.finishWrite(out);}catch(Exception e){f.failWrite(out);throw e;}}
 void restore()throws Exception {nativeIO.requireKsyncAvailable();storage();JSONObject before=state.read("baseline"),journal=state.read("transition");if(before==null||journal==null)throw new UserError(R.string.error_no_saved_previous_configuration);if("restored".equals(journal.optString("phase")))return;String custom=journal.getString("clusterId");String original=before.getJSONObject("cluster").getString("curClusterId");JSONObject live=nativeIO.read(NativeBridge.CLUSTER);String selected=live.getString("curClusterId");if(!selected.equals(custom)&&!selected.equals(original))throw new UserError(R.string.error_another_server_is_selected_inspect_before_restoration);JSONObject installed=state.read("installed-cluster");JSONObject entry=live.getJSONObject("clusterMap").optJSONObject(custom);if(entry!=null&&installed!=null&&!NativeBridge.canonical(entry).equals(NativeBridge.canonical(installed)))throw new UserError(R.string.error_custom_endpoint_changed_outside_powersync_inspect_before);JSONObject installedMarker=state.read("installed-marker");if(installedMarker!=null){byte[] currentMarker=marker().isFile()?Files.readAllBytes(marker().toPath()):null;byte[] previousMarker=before.isNull("marker")?null:Base64.decode(before.getString("marker"),Base64.DEFAULT);boolean originalMarker=java.util.Arrays.equals(currentMarker,previousMarker);boolean ownedMarker=currentMarker!=null&&installedMarker.getString("sha256").equals(NativeBridge.hex(java.security.MessageDigest.getInstance("SHA-256").digest(currentMarker)));if(!originalMarker&&!ownedMarker)throw new UserError(R.string.error_native_marker_changed_outside_powersync_preserve_it);}
  phase(journal,"restore_requested");nativeIO.flags(new JSONObject().put("notes","false").put("reader","false"));if(!live.getJSONObject("clusterMap").has(original)){live.getJSONObject("clusterMap").put(original,before.getJSONObject("cluster").getJSONObject("clusterMap").get(original));nativeIO.write(NativeBridge.CLUSTER,live);}if(!original.equals(selected))nativeIO.switchCluster(original);JSONObject map=nativeIO.read(NativeBridge.CLUSTER);map.getJSONObject("clusterMap").remove(custom);map.put("curClusterId",original);if(map.has("clusterMapSize"))map.put("clusterMapSize",map.getJSONObject("clusterMap").length());nativeIO.write(NativeBridge.CLUSTER,map);
  if(installedMarker!=null){if(before.isNull("marker")){new AtomicFile(marker()).delete();if(marker().exists())throw new UserError(R.string.error_cannot_remove_powersync_marker);}else writeMarker(Base64.decode(before.getString("marker"),Base64.DEFAULT));}
  nativeIO.repairMissingCatalog(before.getJSONArray("catalog"));boolean accountRestored=before.has("accountCache")?new NativeAccountBackup(nativeIO).restore(before.getJSONObject("account"),before.getJSONArray("accountCache"),journal.getString("nativeUid"),custom):nativeIO.restoreAccount(before.getJSONObject("account"),journal.getString("nativeUid"));JSONObject refreshed=nativeIO.read(NativeBridge.CLUSTER);refreshed.getJSONObject("clusterMap").remove(custom);if(refreshed.has("clusterMapSize"))refreshed.put("clusterMapSize",refreshed.getJSONObject("clusterMap").length());nativeIO.write(NativeBridge.CLUSTER,refreshed);nativeIO.repairMissingCatalog(before.getJSONArray("catalog"));nativeIO.flags(accountRestored?before.getJSONObject("flags"):new JSONObject().put("notes","false").put("reader","false"));phase(journal,accountRestored?"restored":"needs_signin");}
 // Endpoint updates change only the endpoint values of the connected custom cluster: no
 // switch, no sign-in, same cluster identity. Fails closed and rolls back on any mismatch.
 JSONObject endpointPlan()throws Exception {
  storage();nativeIO.requireKsyncAvailable();JSONObject p=state.read("profile"),j=state.read("transition");
  if(p==null||j==null||!"active".equals(j.optString("phase")))throw new UserError(R.string.error_connect_to_your_enrolled_server_before_updating);
  JSONObject pending=state.read("endpoint-update");if(pending!=null&&!java.util.Set.of("applied","rolled_back").contains(pending.optString("phase")))throw new UserError(R.string.error_a_previous_endpoint_update_was_interrupted_inspect);
  String id=p.getJSONObject("cluster").getString("clusterId");JSONObject live=nativeIO.read(NativeBridge.CLUSTER);
  if(!id.equals(live.getString("curClusterId")))throw new UserError(R.string.error_another_server_is_selected_inspect_before_updating);
  JSONObject a=nativeIO.account();if(!a.optBoolean("loggedIn")||!p.getString("nativeUid").equals(a.optString("uid")))throw new UserError(R.string.error_native_account_differs_from_your_enrolled_library);
  JSONObject entry=live.getJSONObject("clusterMap").optJSONObject(id),installed=state.read("installed-cluster");
  if(entry==null||installed==null||!NativeBridge.canonical(entry).equals(NativeBridge.canonical(installed)))throw new UserError(R.string.error_custom_endpoint_changed_outside_powersync_inspect_before_2);
  JSONObject reply=ServerClient.request(p.getString("serverURL")+"/api/v1/boox/device/endpoints","GET",null,p.getString("companionToken"));
  if(!p.getString("libraryId").equals(reply.getString("libraryId"))||!p.getString("nativeUid").equals(reply.getString("nativeUid")))throw new UserError(R.string.error_server_library_identity_differs);
  JSONObject next=reply.getJSONObject("cluster");if(!id.equals(next.getString("clusterId"))||!id.equals(next.getString("lang")))throw new UserError(R.string.error_server_changed_the_cluster_identity_enroll_again);
  JSONObject candidate=EndpointProfile.validate(p.getString("serverURL"),new JSONObject(p.toString()).put("cluster",next));
  JSONArray changes=new JSONArray();JSONObject updated=EndpointProfile.updatedEntry(entry,next,changes);
  return new JSONObject().put("clusterId",id).put("changes",changes).put("entry",entry).put("updated",updated).put("profile",candidate);
 }
 String applyEndpointUpdate()throws Exception {
  JSONObject plan=endpointPlan();if(plan.getJSONArray("changes").length()==0)return context.getString(R.string.endpoints_current);
  String id=plan.getString("clusterId");JSONObject profile=plan.getJSONObject("profile"),updated=plan.getJSONObject("updated");
  byte[] markerBytes=marker().isFile()?Files.readAllBytes(marker().toPath()):null;if(markerBytes==null||markerBytes.length>1048576)throw new UserError(R.string.error_native_server_marker_is_missing_or_too);
  JSONObject installedMarker=state.read("installed-marker");if(installedMarker==null||!installedMarker.getString("sha256").equals(NativeBridge.hex(java.security.MessageDigest.getInstance("SHA-256").digest(markerBytes))))throw new UserError(R.string.error_native_marker_changed_outside_powersync_inspect_before);
  byte[] nextMarker=EndpointProfile.replaceMarker(markerBytes,profile);
  JSONObject flags=nativeIO.flags();JSONObject journal=new JSONObject().put("phase","prepared").put("clusterId",id).put("changes",plan.getJSONArray("changes")).put("previousEntry",plan.getJSONObject("entry")).put("previousMarker",Base64.encodeToString(markerBytes,Base64.NO_WRAP)).put("flags",flags);state.write("endpoint-update",journal);
  try{
   endpointPhase(journal,"pause_requested");nativeIO.flags(new JSONObject().put("notes","false").put("reader","false"));
   endpointPhase(journal,"configure_requested");JSONObject map=nativeIO.read(NativeBridge.CLUSTER);map.getJSONObject("clusterMap").put(id,updated);nativeIO.write(NativeBridge.CLUSTER,map);writeMarker(nextMarker);
   endpointPhase(journal,"refresh_requested");nativeIO.updateClusters();
   JSONObject live=nativeIO.read(NativeBridge.CLUSTER);JSONObject refreshed=live.getJSONObject("clusterMap").getJSONObject(id);
   if(!id.equals(live.getString("curClusterId")))throw new UserError(R.string.error_native_refresh_selected_another_server);
   if(!NativeBridge.canonical(refreshed.getJSONObject("clusterHost")).equals(NativeBridge.canonical(updated.getJSONObject("clusterHost")))||!NativeBridge.canonical(refreshed.getJSONObject("ossConfig")).equals(NativeBridge.canonical(updated.getJSONObject("ossConfig"))))throw new UserError(R.string.error_native_refresh_did_not_keep_the_updated);
   JSONObject a=nativeIO.account();if(!a.optBoolean("loggedIn")||!profile.getString("nativeUid").equals(a.optString("uid")))throw new UserError(R.string.error_native_account_changed_during_the_update);
   state.write("installed-cluster",refreshed);state.write("installed-marker",new JSONObject().put("sha256",NativeBridge.hex(java.security.MessageDigest.getInstance("SHA-256").digest(nextMarker))));state.write("profile",profile);
   nativeIO.flags(flags);endpointPhase(journal,"applied");
   // Notes, the launcher and Reader cache the cluster in memory and reload it on these broadcasts.
   for(String action:new String[]{"SERVER_INFO_CHANGED_ACTION","ONYX_MATERIAL_HOST_BASEURL_CHANGED_ACTION"})context.sendBroadcast(new android.content.Intent(action));
  }catch(Exception e){
   // A failed refresh saves nothing natively, so restoring our two copies is enough; a refresh
   // that does succeed here must not be what decides whether sync settings come back.
   try{JSONObject map=nativeIO.read(NativeBridge.CLUSTER);map.getJSONObject("clusterMap").put(id,plan.getJSONObject("entry"));nativeIO.write(NativeBridge.CLUSTER,map);writeMarker(markerBytes);try{nativeIO.updateClusters();}catch(Exception refresh){journal.put("rollbackRefresh","unavailable");}nativeIO.flags(flags);journal.put("failure","Update failed; previous endpoints restored");endpointPhase(journal,"rolled_back");}
   catch(Exception rollback){journal.put("failure","Update and rollback both failed; synchronization remains paused");endpointPhase(journal,"failed");}
   throw e;
  }
  int n=plan.getJSONArray("changes").length();return context.getResources().getQuantityString(R.plurals.endpoints_updated,n,n);
 }
 private void endpointPhase(JSONObject journal,String phase)throws Exception {journal.put("phase",phase).put("updatedAt",System.currentTimeMillis());state.write("endpoint-update",journal);}
 JSONObject recoveryStatus()throws Exception {JSONObject p=state.read("profile");if(p==null)throw new UserError(R.string.error_enroll_a_server_first);return ServerClient.request(p.getString("serverURL")+"/api/v1/boox/device/status","GET",null,p.getString("companionToken"));}
 private JSONObject audioProfile()throws Exception {
  storage();nativeIO.requireKsyncAvailable();JSONObject p=state.read("profile"),j=state.read("transition");
  if(p==null||j==null||!"active".equals(j.optString("phase")))throw new UserError(R.string.error_connect_to_your_enrolled_server_before_recovering);
  JSONObject a=nativeIO.account();
  if(!a.optBoolean("loggedIn")||!p.getString("nativeUid").equals(a.optString("uid"))||!p.getJSONObject("cluster").getString("clusterId").equals(nativeIO.read(NativeBridge.CLUSTER).getString("curClusterId")))throw new UserError(R.string.error_native_account_or_selected_server_changed);
  return p;
 }
 JSONArray audioRecoveries()throws Exception {
  JSONObject p=audioProfile();JSONArray all=new JSONArray();String after="";
  do {JSONObject reply=ServerClient.request(p.getString("serverURL")+"/api/v1/boox/device/audio-recovery?after="+after,"GET",null,p.getString("companionToken"));
   if(!p.getString("nativeUid").equals(reply.getString("nativeUid")))throw new UserError(R.string.error_audio_library_identity_differs);
   JSONArray items=reply.getJSONArray("items");for(int i=0;i<items.length();i++)all.put(items.getJSONObject(i));
   String next=reply.getString("next");if(!next.isEmpty()){java.util.UUID.fromString(next);if(next.compareTo(after)<=0)throw new UserError(R.string.error_invalid_audio_recovery_cursor);}after=next;
   if(all.length()>1000)throw new UserError(R.string.error_too_many_audio_repairs_review_this_library);
  }while(!after.isEmpty());return all;
 }
 AudioCacheRepair audioPacket(String id)throws Exception {
  java.util.UUID.fromString(id);JSONObject p=audioProfile();
  AudioCacheRepair repair=new AudioCacheRepair(ServerClient.request(p.getString("serverURL")+"/api/v1/boox/device/audio-recovery?operation="+id,"GET",null,p.getString("companionToken"),24*1024*1024),p.getString("nativeUid"));
  try(android.database.Cursor c=context.getContentResolver().query(android.net.Uri.parse("content://com.onyx.android.sdk.note.ContentProvider/NoteModel"),new String[]{"uniqueId"},"uniqueId=? AND userId=? AND type=1 AND status=1",new String[]{repair.document,repair.uid},null)){
   if(c==null||c.getCount()!=1)throw new UserError(R.string.error_the_active_notebook_is_not_available_in);
  }
  return repair;
 }
 String inspectAudio(AudioCacheRepair repair)throws Exception {return repair.inspect(Environment.getExternalStorageDirectory().toPath());}
 void repairAudio(AudioCacheRepair selected)throws Exception {
  AudioCacheRepair current=audioPacket(selected.operation);
  if(!current.sha.equals(selected.sha)||!current.damagedSHA.equals(selected.damagedSHA))throw new UserError(R.string.error_server_repair_changed_review_it_again);
  current.apply(Environment.getExternalStorageDirectory().toPath(),new File(context.getFilesDir(),"audio-recovery-backups").toPath());
 }

}
