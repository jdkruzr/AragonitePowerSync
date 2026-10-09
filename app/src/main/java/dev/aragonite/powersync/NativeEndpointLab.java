package dev.aragonite.powersync;

import android.content.Context;
import android.os.Environment;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Fixed local-lab endpoints; reversible provisioning entirely in the app UID. */
final class NativeEndpointLab {
 private final Context context;
 private final NativeSystemConfigProbe io=new NativeSystemConfigProbe();
 private static final String CLUSTER="ONYX_CLUSTER_SYSTEM_KEY",SYSTEM="ONYX_SYSTEM_CONFIG_KEY";
 NativeEndpointLab(Context context){this.context=context;}
 JSONObject status() throws Exception {
  android.app.KeyguardManager keyguard=context.getSystemService(android.app.KeyguardManager.class);
  return new JSONObject().put("clusterId",io.read().getString("curClusterId")).put("deviceSecure",keyguard.isDeviceSecure()).put("deviceLocked",keyguard.isDeviceLocked()).put("uid",android.os.Process.myUid());
 }
 private File state(){return new File(context.getFilesDir(),"endpoint-app-baseline.json");}
 private JSONObject baseline() throws Exception {
  if(!state().isFile())throw new IOException("Provision app endpoint first");
  return new JSONObject(new String(java.nio.file.Files.readAllBytes(state().toPath()),StandardCharsets.UTF_8));
 }
 private JSONObject definition() throws Exception {
  String address="127.0.0.1",tlsSuffix="";boolean tls=false,accountTls=true;
  File override=new File(context.getFilesDir(),"endpoint-lab-host.json");
  if(override.isFile()){
   if(override.length()>256)throw new IOException("Lab host definition exceeds bound");
   JSONObject requested=new JSONObject(new String(java.nio.file.Files.readAllBytes(override.toPath()),StandardCharsets.UTF_8));address=requested.getString("host");tls=requested.optBoolean("https",false);
   if(tls){
    if(requested.optBoolean("certificateNegativeLab")){
     if(!"192.168.8.95".equals(address)||requested.optInt("tlsPort")!=19443)throw new IOException("Fixed certificate-negative LAN target required");
     tlsSuffix=":19443";accountTls=requested.optBoolean("accountHttps",true);
    }else if(!address.matches("[a-z0-9-]+\\.trycloudflare\\.com"))throw new IOException("Disposable tunnel hostname required");
   }else{
   if(!address.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}"))throw new IOException("Numeric lab IPv4 required");
   java.net.InetAddress parsed=java.net.InetAddress.getByName(address);
   if(parsed.isAnyLocalAddress()||(!parsed.isSiteLocalAddress()&&!parsed.isLoopbackAddress()))throw new IOException("Private lab address required");
   address=parsed.getHostAddress();}
  }
  String http=tls&&accountTls?"https://"+address+tlsSuffix:"http://"+address+":18765",ws=tls?"wss://"+address+tlsSuffix+"/neocloud":"ws://"+address+":18766/neocloud";
  JSONObject host=new JSONObject(),oss=new JSONObject();
  for(String key:new String[]{"onyxHostBaseUrl","onyxCloudDataHostBaseUrl","onyxLogHostBaseUrl","onyxContentHostBaseUrl","onyxSend2BooxHostBaseUrl","shopBookUrl","onyxMaterialHostBaseUrl"})host.put(key,http);
  host.put("onyxCouchbaseWSSHost",ws).put("onyxMessageReplicator",ws);
  for(String key:new String[]{"ossNoteEndPoint","ossDeprecatedEndPoint","ossAIEndPoint","ossLogEndpoint","ossTestEndpoint"})oss.put(key,http);
  for(String key:new String[]{"ossNoteBucket","ossDeprecatedBucket","ossAIBucket","ossLogBucket","ossTestBucket"})oss.put(key,"powersync-lab");
  return new JSONObject().put("clusterId",NativeClusterLab.LAB_ID).put("lang",NativeClusterLab.LAB_ID).put("name","PowerSync local lab").put("region","POWERSYNC_LAB")
   .put("clusterHost",host).put("ossConfig",oss).put("couchbase",new JSONObject().put("onyxCouchbaseWSSHost",ws)).put("messageCouchbase",new JSONObject().put("onyxCouchbaseWSSHost",ws));
 }
 JSONObject provision(boolean marker) throws Exception {
  JSONObject before=io.read();
  if(!"en".equals(before.getString("curClusterId")))throw new IOException("Requires original en cluster before provisioning");
  if(!new NativeGatewayLab(context).preservation().getBoolean("otherRowsPreserved"))throw new IOException("Library preservation check failed");
  File test=new File(Environment.getExternalStorageDirectory(),"onyx_test_server");
  if(!state().exists()){
   if(test.exists()||before.getJSONObject("clusterMap").has(NativeClusterLab.LAB_ID))throw new IOException("Unexpected existing lab setup");
   ReportFiles.write(context,state().getName(),new JSONObject().put("cluster",before).put("system",io.read(SYSTEM)).put("markerOriginallyPresent",false));
  }
  if(marker){
   if(!Environment.isExternalStorageManager())throw new IOException("All Files Access required for test definition");
   JSONObject d=definition();JSONObject server=new JSONObject().put("serverId",NativeClusterLab.LAB_ID).put("lang",NativeClusterLab.LAB_ID).put("name",d.getString("name")).put("region",d.getString("region")).put("host",d.getJSONObject("clusterHost")).put("ossConfig",d.getJSONObject("ossConfig"));
   try(FileOutputStream out=new FileOutputStream(test)){out.write(new JSONArray().put(server).toString().getBytes(StandardCharsets.UTF_8));out.getFD().sync();}
  }else if(test.exists())throw new IOException("Marker-free experiment requires marker absence");
  before.getJSONObject("clusterMap").put(NativeClusterLab.LAB_ID,definition());io.write(CLUSTER,before);
  return new JSONObject().put("uid",android.os.Process.myUid()).put("provisioned",true).put("markerInstalledByApp",marker).put("liveClusterId",io.read().getString("curClusterId"));
 }
 JSONObject reassert() throws Exception {
  JSONObject d=definition();
  File marker=new File(Environment.getExternalStorageDirectory(),"onyx_test_server");
  if(marker.exists()){
   if(baseline().optBoolean("markerOriginallyPresent",true))throw new IOException("Only app-owned lab marker may be refreshed");
   JSONObject server=new JSONObject().put("serverId",NativeClusterLab.LAB_ID).put("lang",NativeClusterLab.LAB_ID).put("name",d.getString("name")).put("region",d.getString("region")).put("host",d.getJSONObject("clusterHost")).put("ossConfig",d.getJSONObject("ossConfig"));
   try(FileOutputStream out=new FileOutputStream(marker)){out.write(new JSONArray().put(server).toString().getBytes(StandardCharsets.UTF_8));out.getFD().sync();}
  }
  JSONObject candidate=baseline().getJSONObject("cluster");candidate.getJSONObject("clusterMap").put(NativeClusterLab.LAB_ID,d);candidate.put("curClusterId",NativeClusterLab.LAB_ID);io.write(CLUSTER,candidate);
  return new JSONObject().put("liveClusterId",io.read().getString("curClusterId")).put("uid",android.os.Process.myUid());
 }
 JSONObject flags(boolean restore) throws Exception {
  JSONObject original=baseline().getJSONObject("system"),current=io.read(SYSTEM);
  if(!restore&&!NativeClusterLab.LAB_ID.equals(io.read().getString("curClusterId")))throw new IOException("Note enable requires lab cluster");
  for(String key:new String[]{"key_enable_sync_note_data","key_enable_sync_reader_data"})current.getJSONObject("configMap").put(key,restore?original.getJSONObject("configMap").get(key):("key_enable_sync_note_data".equals(key)?"true":"false"));
  io.write(SYSTEM,current);context.sendBroadcast(new android.content.Intent("com.onyx.action.system.config.changed"));return new JSONObject().put("restored",restore).put("verified",true);
 }
 JSONObject restore() throws Exception {
  JSONObject saved=baseline();
  if(!saved.getJSONObject("cluster").getString("curClusterId").equals(io.read().getString("curClusterId")))throw new IOException("Native switch back required before exact restore");
  io.write(CLUSTER,saved.getJSONObject("cluster"));flags(true);
  File marker=new File(Environment.getExternalStorageDirectory(),"onyx_test_server");if(marker.exists()&&!marker.delete())throw new IOException("Cannot remove app test marker");
  return new JSONObject().put("exactClusterRestored",true).put("markerRemoved",!marker.exists()).put("uid",android.os.Process.myUid());
 }
}
