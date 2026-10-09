package dev.aragonite.powersync;

import android.content.Context;
import android.os.Environment;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import org.json.*;

/** Explicit recovery of one owned lab rich-text shape; never a general restore. */
final class NativeRichTextRecoveryLab {
 private static final String DOC="f7f635344f624650aa300f84d114a1d3",SHAPE="c57e650a-6226-4ab5-a0a9-5e6830f40c84";
 private final Context context;
 NativeRichTextRecoveryLab(Context context){this.context=context;}
 private static String str(Map<Integer,Object> row,int field) throws Exception {
  if(!(row.get(field) instanceof byte[]))throw new IOException("Missing recovery string");
  return new String((byte[])row.get(field),StandardCharsets.UTF_8);
 }
 private static Map<Integer,Object> shape(byte[] raw) throws Exception {
  try(ZipInputStream z=new ZipInputStream(new ByteArrayInputStream(raw))){
   if(z.getNextEntry()==null)throw new IOException("Missing shape envelope");
   Map<Integer,Object> env=NativeInk.fields(NativeInk.read(z));
   if(z.getNextEntry()!=null||env.size()!=1||!(env.get(1) instanceof byte[]))throw new IOException("Ambiguous shape envelope");
   return NativeInk.fields((byte[])env.get(1));
  }
 }
 private static void textOnly(Object node,int depth) throws Exception {
  if(depth>64)throw new IOException("Recovery content nesting exceeds bound");
  if(node instanceof JSONObject object){String type=object.optString("type");if(Arrays.asList("image","audio","video","file","attachment").contains(type))throw new IOException("Recovery attachments need a closed asset graph");Iterator<String> keys=object.keys();while(keys.hasNext())textOnly(object.get(keys.next()),depth+1);}
  else if(node instanceof JSONArray array)for(int i=0;i<array.length();i++)textOnly(array.get(i),depth+1);
 }
 private JSONObject current() throws Exception {
  JSONObject report=new NativeGatewayLab(context).exportShared();
  try(ZipFile z=new ZipFile(report.getString("archivePath"))){
   Map<Integer,Object> found=null;Enumeration<? extends ZipEntry> entries=z.entries();
   while(entries.hasMoreElements()){
    ZipEntry e=entries.nextElement();if(!e.getName().startsWith(DOC+"/shape/")||!e.getName().endsWith(".zip"))continue;
    Map<Integer,Object> row;try(InputStream in=z.getInputStream(e)){row=shape(NativeInk.read(in));}
    if(!Long.valueOf(29).equals(row.get(12)))continue;
    if(found!=null||!SHAPE.equals(str(row,1)))throw new IOException("Ambiguous current text shape");found=row;
   }
   if(found==null)throw new IOException("Missing current text shape");
   JSONObject ref=new JSONObject(str(found,14));String id=ref.getString("uniqueId");
   if(!DOC.equals(ref.getString("documentId"))||!id.matches("[0-9a-f-]{36}")||!("/"+id+".html").equals(ref.getString("relativePath")))throw new IOException("Unsafe current text reference");
   ZipEntry e=z.getEntry(DOC+"/resource/data/"+id+".html");if(e==null)throw new IOException("Missing active text asset");
   byte[] raw;try(InputStream in=z.getInputStream(e)){raw=NativeInk.read(in);}
   return new JSONObject().put("resourceId",id).put("activeSha256",NativeInk.digest(raw));
  }
 }
 JSONObject apply() throws Exception {
  NativeSystemConfigProbe config=new NativeSystemConfigProbe();
  if(!NativeClusterLab.LAB_ID.equals(config.read().getString("curClusterId")))throw new IOException("Lab cluster required");
  if(!"false".equals(config.read("ONYX_SYSTEM_CONFIG_KEY").getJSONObject("configMap").optString("key_enable_sync_note_data")))throw new IOException("Pause native note sync before recovery");
  JSONObject note=new NativeGatewayLab(context).selectedCapture().getJSONObject("note");
  if(!DOC.equals(note.getString("uniqueId"))||!"powersync-lab".equals(note.optString("userId"))||note.optInt("activeScene")!=1||note.optInt("status")!=1)throw new IOException("Owned live text fixture required");
  File requestFile=new File(context.getFilesDir(),"richtext-recovery-request.json");
  if(requestFile.length()>8192)throw new IOException("Oversized recovery request");
  JSONObject request=new JSONObject(new String(java.nio.file.Files.readAllBytes(requestFile.toPath()),StandardCharsets.UTF_8));
  if(!request.optBoolean("explicitRecovery")||!request.optBoolean("textOnly")||!DOC.equals(request.getString("documentId"))||!SHAPE.equals(request.getString("shapeId")))throw new IOException("Explicit scoped recovery required");
  File exchange=new File(Environment.getExternalStorageDirectory(),"PowerSyncProbe/richtext-recovery.zip");byte[] incoming;
  try(InputStream in=new FileInputStream(exchange)){incoming=NativeInk.read(in);}
  String receipt=NativeInk.digest(incoming);if(!receipt.equals(request.getString("payloadSha256")))throw new IOException("Recovery payload hash mismatch");
  File inbox=new File(context.getFilesDir(),"richtext-recovery-inbox");if(!inbox.isDirectory()&&!inbox.mkdirs())throw new IOException("Cannot create private recovery inbox");
  File source=new File(inbox,receipt+".zip");
  if(!source.exists()){AtomicFile saved=new AtomicFile(source);FileOutputStream out=saved.startWrite();try{out.write(incoming);saved.finishWrite(out);}catch(Exception e){saved.failWrite(out);throw e;}}
  else if(!receipt.equals(NativeInk.digest(java.nio.file.Files.readAllBytes(source.toPath()))))throw new IOException("Recovery receipt corrupted");
  NativeArchive.inspect(source);String journalName="richtext-recovery-"+receipt+".json";
  try(ZipFile z=new ZipFile(source)){
   List<String> paths=new ArrayList<>();Enumeration<? extends ZipEntry> entries=z.entries();String q=java.util.regex.Pattern.quote(DOC);
   while(entries.hasMoreElements()){
    ZipEntry e=entries.nextElement();String n=e.getName();
    if(e.isDirectory()||!(n.matches("document/"+q+"/resource/data/[0-9a-f-]{36}\\.html")||n.matches("document/"+q+"/resource/pb/[0-9a-f-]{36}#[0-9]+")||n.matches("document/"+q+"/shape/[0-9a-f-]{36}#[0-9a-f-]{36}#[0-9]+\\.zip")||n.matches("point/"+q+"/[0-9a-f-]{36}/[0-9a-f-]{36}#[0-9a-f-]{36}#points")))throw new IOException("Recovery path outside fixture");paths.add(n);
   }
   if(paths.size()!=4)throw new IOException("Require one complete text revision");
   String html=one(paths,"/resource/data/"),resourcePath=one(paths,"/resource/pb/"),shapePath=one(paths,"/shape/"),pointPath=one(paths,"point/");
   String[] shapeParts=shapePath.substring(shapePath.lastIndexOf('/')+1).split("#");String page=shapeParts[0];String[] pointParts=pointPath.substring(pointPath.lastIndexOf('/')+1).split("#");
   if(!page.equals(request.getString("pageId"))||!page.equals(pointParts[0])||!page.equals(pointPath.split("/")[2]))throw new IOException("Recovery page mismatch");
   JSONArray pages=new JSONObject(note.getString("richTextPageNameList")).getJSONArray("pageNameList");if(pages.length()!=1||!page.equals(pages.getString(0)))throw new IOException("Recovery page is not current text page");
   byte[] shapes=read(z,shapePath),points=read(z,pointPath),raw=read(z,html);NativeInk.validate(shapes,points,page,pointParts[1]);
   Map<Integer,Object> row=shape(shapes);if(!Long.valueOf(29).equals(row.get(12))||!SHAPE.equals(str(row,1))||!shapeParts[1].equals(str(row,18)))throw new IOException("Recovery shape mismatch");
   Map<Integer,Object> env=NativeInk.fields(read(z,resourcePath));if(env.size()!=1||!(env.get(1) instanceof byte[]))throw new IOException("Recovery resource envelope mismatch");Map<Integer,Object> res=NativeInk.fields((byte[])env.get(1));
   JSONObject ref=new JSONObject(str(row,14));String uid=str(res,1),relative=str(res,8),rev=str(res,11);
   if(!uid.equals(request.getString("newResourceId"))||!DOC.equals(str(res,2))||!Long.valueOf(3).equals(res.get(7))||!Long.valueOf(0).equals(res.get(13))||!("/"+uid+".html").equals(relative)||!rev.equals(resourcePath.substring(resourcePath.lastIndexOf('/')+1).split("#")[0])||!uid.equals(ref.getString("uniqueId"))||!DOC.equals(ref.getString("documentId"))||!relative.equals(ref.getString("relativePath"))||!rev.equals(ref.getString("revisionId"))||ref.optInt("type")!=3||ref.optInt("status")!=0||!html.equals("document/"+DOC+"/resource/data"+relative))throw new IOException("Recovery resource reference mismatch");
   JSONObject body=new JSONObject(new String(raw,StandardCharsets.UTF_8));if(body.optInt("docVersion")!=2||!"doc".equals(body.getJSONObject("content").optString("type"))||!NativeInk.digest(raw).equals(request.getString("retainedSha256")))throw new IOException("Recovery content mismatch");
   textOnly(body.getJSONObject("content"),0);
   JSONObject before=current();
   if(uid.equals(before.getString("resourceId"))&&request.getString("retainedSha256").equals(before.getString("activeSha256"))&&new File(context.getFilesDir(),journalName).exists())return new JSONObject().put("alreadyApplied",true).put("receipt",receipt).put("nativeEditorVerified",false);
   if(!request.getString("expectedResourceId").equals(before.getString("resourceId"))||!request.getString("expectedActiveSha256").equals(before.getString("activeSha256")))throw new IOException("Active text changed since recovery was prepared");
   File root=new File(Environment.getExternalStorageDirectory(),".ksync").getCanonicalFile();
   for(String path:paths){File target=new File(root,path).getCanonicalFile();if(!target.toPath().startsWith(root.toPath()))throw new IOException("Unsafe recovery target");if(target.exists()&&!Arrays.equals(java.nio.file.Files.readAllBytes(target.toPath()),read(z,path)))throw new IOException("Immutable recovery revision conflict");}
   paths.sort(Comparator.comparingInt(n->n.contains("/resource/data/")?0:n.contains("/resource/pb/")?1:n.startsWith("point/")?2:3));
   JSONObject journal=new JSONObject().put("request",request).put("receipt",receipt).put("before",before).put("phase","prepared");ReportFiles.write(context,journalName,journal);
   for(String path:paths){File target=new File(root,path).getCanonicalFile();if(target.exists())continue;if(!target.getParentFile().isDirectory()&&!target.getParentFile().mkdirs())throw new IOException("Cannot stage recovery directory");AtomicFile saved=new AtomicFile(target);FileOutputStream out=saved.startWrite();try{out.write(read(z,path));saved.finishWrite(out);}catch(Exception e){saved.failWrite(out);throw e;}}
   ReportFiles.write(context,journalName,journal.put("phase","staged"));new NativeNotes(context).notifyFixtureData(DOC);ReportFiles.write(context,journalName,journal.put("phase","native_import_requested"));
   return new JSONObject().put("documentId",DOC).put("shapeId",SHAPE).put("newResourceId",uid).put("staged",true).put("receipt",receipt).put("nativeImportRequested",true).put("nativeEditorVerified",false);
  }
 }
 private static String one(List<String> paths,String token) throws Exception{List<String> found=new ArrayList<>();for(String p:paths)if(p.contains(token))found.add(p);if(found.size()!=1)throw new IOException("Ambiguous recovery entry");return found.get(0);}
 private static byte[] read(ZipFile z,String name) throws Exception{try(InputStream in=z.getInputStream(z.getEntry(name))){return NativeInk.read(in);}}
}
