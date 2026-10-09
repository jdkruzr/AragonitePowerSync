package dev.aragonite.powersync;

import android.os.Process;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Calls the captured system ABI from the app process, without su/run-as/shell. */
final class NativeSystemConfigProbe {
 private static String canonical(Object value) throws Exception {
  if(value instanceof JSONObject object){java.util.TreeMap<String,String> sorted=new java.util.TreeMap<>();java.util.Iterator<String> keys=object.keys();while(keys.hasNext()){String key=keys.next();sorted.put(key,canonical(object.get(key)));}return sorted.toString();}
  if(value instanceof org.json.JSONArray array){ArrayList<String> entries=new ArrayList<>();for(int i=0;i<array.length();i++)entries.add(canonical(array.get(i)));return entries.toString();}
  return new org.json.JSONArray().put(value).toString();
 }
 private static final String KEY="ONYX_CLUSTER_SYSTEM_KEY";
 private byte[] call(String... args) throws Exception {
  ArrayList<String> command=new ArrayList<>(Arrays.asList("/system/bin/service","call","onyx_device_service"));
  command.addAll(Arrays.asList(args));
  java.lang.Process child=new ProcessBuilder(command).redirectErrorStream(true).start();
  if(!child.waitFor(10,TimeUnit.SECONDS)){child.destroyForcibly();throw new IOException("Service command timeout");}
  java.io.ByteArrayOutputStream captured=new java.io.ByteArrayOutputStream();
  byte[] buffer=new byte[4096];int count;while((count=child.getInputStream().read(buffer))!=-1){captured.write(buffer,0,count);if(captured.size()>1048576)throw new IOException("Oversized service reply");}
  byte[] output=captured.toByteArray();
  if(output.length>1048576)throw new IOException("Oversized service reply");
  java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
  Matcher lines=Pattern.compile("(?m)^\\s*0x[0-9a-fA-F]+:\\s*((?:[0-9a-fA-F]{8}(?:\\s+|$)){1,4})").matcher(new String(output,StandardCharsets.UTF_8));
  while(lines.find())for(String word:lines.group(1).trim().split("\\s+"))bytes.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int)Long.parseLong(word,16)).array());
  if(bytes.size()==0){Matcher inline=Pattern.compile("Parcel\\(\\s*((?:[0-9a-fA-F]{8}\\s+)+)").matcher(new String(output,StandardCharsets.UTF_8));if(inline.find())for(String word:inline.group(1).trim().split("\\s+"))bytes.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int)Long.parseLong(word,16)).array());}
  byte[] reply=bytes.toByteArray();
  if(reply.length<8)throw new IOException("No system Binder parcel; exit="+child.exitValue());
  if(ByteBuffer.wrap(reply).order(ByteOrder.LITTLE_ENDIAN).getInt()!=0)throw new IOException("System Binder exception code="+ByteBuffer.wrap(reply).order(ByteOrder.LITTLE_ENDIAN).getInt());
  return reply;
 }
 JSONObject read(String key) throws Exception {
  if(android.os.Build.VERSION.SDK_INT<35 || !("Palma2_Pro_C".equals(android.os.Build.MODEL)||"Go103_2Lumi".equals(android.os.Build.MODEL)))throw new IOException("System ABI qualified only on the two API 35 lab devices");
  if(!KEY.equals(key)&&!"ONYX_SYSTEM_CONFIG_KEY".equals(key))throw new IOException("Unsupported system key");
  byte[] reply=call("3","s16",key);
  int length=ByteBuffer.wrap(reply).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
  if(length<0||length>524288||8L+length*2L>reply.length)throw new IOException("Invalid system configuration reply");
  return new JSONObject(new String(reply,8,length*2,StandardCharsets.UTF_16LE));
 }
 JSONObject read() throws Exception {return read(KEY);}
 void write(String key,JSONObject value) throws Exception {
  read(key);byte[] reply=call("4","s16",key,"s16",value.toString());
  if(ByteBuffer.wrap(reply).order(ByteOrder.LITTLE_ENDIAN).getInt(4)==0 || !canonical(read(key)).equals(canonical(value)))throw new IOException("System write/readback failed");
 }
 JSONObject run() throws Exception {
  if(android.os.Build.VERSION.SDK_INT<35)throw new IOException("Captured system ABI qualified only on API 35 lab firmware");
  JSONObject report=new JSONObject().put("uid",Process.myUid()).put("pid",Process.myPid())
   .put("method","app ProcessBuilder /system/bin/service; no shell, su, run-as or host write")
   .put("selinuxContext",new String(Files.readAllBytes(java.nio.file.Paths.get("/proc/self/attr/current")),StandardCharsets.UTF_8).trim());
  JSONObject original;
  try{original=read();report.put("liveReadSucceeded",true);}catch(Exception e){return report.put("liveReadSucceeded",false).put("failure",e.toString());}
  if(!"en".equals(original.optString("curClusterId")))throw new IOException("Probe requires restored en baseline");
  String id="powersync_uid_probe";
  if(original.getJSONObject("clusterMap").has(id))throw new IOException("Probe target already exists");
  JSONObject candidate=new JSONObject(original.toString());
  candidate.getJSONObject("clusterMap").put(id,new JSONObject().put("clusterId",id)
   .put("name","PowerSync unselected UID probe")
   .put("clusterHost",new JSONObject().put("host","http://127.0.0.1:9")));
  try {
   byte[] result=call("4","s16",KEY,"s16",candidate.toString());
   report.put("writeReturnedTrue",ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN).getInt(4)!=0);
   JSONObject observed=read();
   report.put("arbitraryEntryReadBack",canonical(observed).equals(canonical(candidate))).put("activeClusterUnchanged","en".equals(observed.optString("curClusterId")));
  }catch(Exception e){report.put("failure",e.toString());}
  finally {
   call("4","s16",KEY,"s16",original.toString());
   report.put("originalConfigurationRestored",canonical(read()).equals(canonical(original)));
   if(!report.getBoolean("originalConfigurationRestored"))throw new IOException("System configuration restore failed");
  }
  return report;
 }
}
