package dev.aragonite.powersync;

import android.content.*;
import android.os.Environment;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Only operates on the unique disposable fixture recorded by this installation. */
final class FixtureRunner {
 private final Context context;
 private final NativeNotes notes;
 private final SharedPreferences prefs;
 FixtureRunner(Context context) { this(context,"fixture"); }
 FixtureRunner(Context context,String fixture) { this.context=context; notes=new NativeNotes(context); prefs=context.getSharedPreferences(fixture,0); }
 Intent createInkIntent() throws Exception {
  if(prefs.contains("fixtureTitle")) {try{return open();}catch(IOException e){throw new IOException("Ink creation already requested; inspect native creation screen before retry",e);}}
  String title="PowerSync ink "+UUID.randomUUID();
  if(!prefs.edit().putString("fixtureTitle",title).commit()) throw new IOException("Cannot journal ink fixture");
  JSONObject bean=new JSONObject().put("title",title).put("parentUniqueId","").put("create",true).put("createNoteType","SCRIBBLE_NOTE").put("activeScene",0);
  return new Intent().setComponent(new ComponentName(NativeNotes.NOTE_PACKAGE,NativeNotes.NOTE_PACKAGE+".note.ui.ScribbleActivity")).putExtra("OPEN_NOTE_BEAN",bean.toString()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
 }
 JSONObject find() throws Exception {
  String title=prefs.getString("fixtureTitle",null);
  String id=prefs.getString("fixtureId",null);
  if(title == null) throw new IOException("Create a disposable fixture first");
  // The native text importer may append a numeric suffix even to a unique title.
  JSONArray rows=notes.notes(id == null ? "(title=? OR title LIKE ?) AND type=?" : "uniqueId=? AND type=?",
   id == null ? new String[]{title,title+"-%","1"} : new String[]{id,"1"});
  if(rows.length() != 1) throw new IOException("Expected exactly one fixture, found " + rows.length());
  JSONObject row=rows.getJSONObject(0);
  if(id == null) {
   String actual=row.optString("title","");
   if(!actual.equals(title) && !actual.matches(java.util.regex.Pattern.quote(title)+"-[0-9]+")) throw new IOException("Unexpected fixture title");
   if(!prefs.edit().putString("fixtureId",row.getString("uniqueId")).commit()) throw new IOException("Cannot journal fixture ID");
  }
  return row;
 }
 JSONObject create() throws Exception {
  if(prefs.contains("fixtureTitle")) { disableNativeCloud(); return new JSONObject().put("alreadyRequested",true).put("note",find()); }
  if(!Environment.isExternalStorageManager()) throw new IOException("Grant All Files Access first");
  String title="PowerSync fixture " + UUID.randomUUID();
  String text="Disposable PowerSync adapter fixture. Created through native Notes Binder.";
  File exchange=new File(Environment.getExternalStorageDirectory(),"PowerSyncProbe/" + UUID.randomUUID() + ".txt");
  if(!exchange.getParentFile().isDirectory() && !exchange.getParentFile().mkdirs()) throw new IOException("Cannot create exchange directory");
  try(FileOutputStream out=new FileOutputStream(exchange)) { out.write(text.getBytes(StandardCharsets.UTF_8)); out.getFD().sync(); }
  if(!prefs.edit().putString("fixtureTitle",title).putString("exchangeFile",exchange.getAbsolutePath()).commit()) throw new IOException("Cannot journal fixture request");
  JSONObject bean=new JSONObject().put("title",title).put("text",text).put("parentUniqueId","")
   .put("richTextCachePath",exchange.getAbsolutePath()).put("activeScene",1).put("createNoteType","TEXT_NOTE");
  notes.send("CREATE_NOTE_FROM_TEXT",bean,true);
  long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(30);
  while(System.nanoTime()<deadline) {
   try { find(); disableNativeCloud(); return new JSONObject().put("note",find()).put("pages",notes.pages(find().getString("uniqueId"))); }
   catch(IOException e) { Thread.sleep(500); }
  }
  throw new IOException("Native creation not observed; request retained, will not create duplicates");
 }
 JSONObject rename() throws Exception {
  JSONObject row=find(); String id=row.getString("uniqueId");
  String desired=prefs.getString("fixtureTitle","")+" renamed";
  JSONObject bean=new JSONObject().put("documentId",id).put("title",desired).put("parentUniqueId",row.optString("parentUniqueId",""));
  notes.send("RENAME_NOTE",new JSONObject().put("noteBean",bean),false);
  for(int i=0;i<40;i++) { JSONObject current=find(); if(desired.equals(current.optString("title"))) return new JSONObject().put("sameIdentity",id.equals(current.getString("uniqueId"))).put("note",current); Thread.sleep(250); }
  throw new IOException("Rename command not observed");
 }
 JSONObject disableNativeCloud() throws Exception {
  JSONObject row=find();String id=row.getString("uniqueId");
  if(row.optInt("noteSyncStatus",-1)!=0) {
   notes.send("UPDATE_SYNC_STATUS",new JSONObject().put("documentId",id).put("noteSyncStatus",0),false);
   for(int i=0;i<40;i++) {row=find();if(row.optInt("noteSyncStatus",-1)==0) break;Thread.sleep(250);}
  }
  if(row.optInt("noteSyncStatus",-1)!=0) throw new IOException("Native cloud disable not observed");
  return new JSONObject().put("documentId",id).put("nativeCloudSyncDisabled",true);
 }
 JSONObject export() throws Exception {
  return exportRow(find());
 }
 JSONObject exportRow(JSONObject row) throws Exception {
  String token="PowerSync-export-"+UUID.randomUUID();
  java.util.concurrent.CompletableFuture<JSONObject> result=new java.util.concurrent.CompletableFuture<>();
  BroadcastReceiver receiver=new BroadcastReceiver() {
   public void onReceive(Context c,Intent intent) {
    try {
     String raw=intent.getStringExtra("SERVICE_INTENT_RESULT_JSON");
     if(raw == null || raw.length()>1048576) return;
     JSONObject event=new JSONObject(raw);
     if(!token.equals(event.optString("exportTitle"))) return;
     if(event.optInt("status",-1)==0) result.complete(event);
     else if(event.optInt("status",-1)==1) result.completeExceptionally(new IOException("Native export failed"));
    } catch(Exception ignored) { }
   }
  };
  IntentFilter filter=new IntentFilter("com.onyx.android.sdk.note.ui.service.SERVICE_INTENT_RESULT");
  registerNativeResult(receiver,filter);
  try {
   JSONObject args=new JSONObject().put("format",5).put("remoteExport",true)
    .put("documentId",row.getString("uniqueId")).put("parentUniqueId",row.optString("parentUniqueId",""))
    .put("title",token).put("noteTitle",token).put("noteScene",row.optInt("activeScene",1))
    .put("userId",row.optString("userId","")).put("exportInvisibleLayers",true);
   notes.send("EXPORT_NOTE",new JSONObject().put("exportNoteArgs",args),false);
   JSONObject event=result.get(90,java.util.concurrent.TimeUnit.SECONDS);
   String path=event.getJSONObject("exportNoteArgs").getString("exportPath");
   File file=new File(path).getCanonicalFile();
   File storage=Environment.getExternalStorageDirectory().getCanonicalFile();
   if(!file.toPath().startsWith(storage.toPath()) || !file.getName().equals(token+".note")) throw new IOException("Unexpected export path");
   JSONObject manifest=NativeArchive.inspect(file);
   try {manifest.put("inkIdentity",NativeArchive.inkIdentity(file,row.getString("uniqueId")));}
   catch(Exception e) {manifest.put("inkIdentity",new JSONObject().put("supported",false).put("reason",e.toString()));}
   return new JSONObject().put("documentId",row.getString("uniqueId")).put("archivePath",file.getPath())
    .put("archive",manifest).put("completeNativeSnapshot",false);
  } finally { context.unregisterReceiver(receiver); }
 }
 @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
 private void registerNativeResult(BroadcastReceiver receiver,IntentFilter filter) {
  // Flags are explicit on Android 13+. The legacy branch only runs on API 30-32,
  // whose two-argument registration is exported so the vendor UID can respond.
  if(android.os.Build.VERSION.SDK_INT>=33) context.registerReceiver(receiver,filter,Context.RECEIVER_EXPORTED);
  else context.registerReceiver(receiver,filter);
 }
 Intent open() throws Exception { return notes.openIntent(find()); }
 Intent insertImage() throws Exception {
  disableNativeCloud();JSONObject row=find();
  if(prefs.contains("imageRequest")) return notes.openIntent(row);
  File image=new File(Environment.getExternalStorageDirectory(),"PowerSyncProbe/fixture-image-"+UUID.randomUUID()+".png");
  if(!image.getParentFile().isDirectory() && !image.getParentFile().mkdirs()) throw new IOException("Cannot create image exchange directory");
  android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(256,128,android.graphics.Bitmap.Config.ARGB_8888);
  android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap);canvas.drawColor(android.graphics.Color.WHITE);
  android.graphics.Paint paint=new android.graphics.Paint();paint.setColor(android.graphics.Color.BLACK);paint.setStrokeWidth(6);
  canvas.drawLine(0,0,256,128,paint);canvas.drawLine(0,128,256,0,paint);
  paint.setStyle(android.graphics.Paint.Style.STROKE);canvas.drawRect(3,3,253,125,paint);
  try(FileOutputStream out=new FileOutputStream(image)) {
   if(!bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out)) throw new IOException("Cannot encode fixture image");out.getFD().sync();
  } finally {bitmap.recycle();}
  if(!prefs.edit().putString("imageRequest",image.getPath()).commit()) throw new IOException("Cannot journal image insertion");
  Intent intent=notes.openIntent(row);JSONObject bean=new JSONObject(intent.getStringExtra("OPEN_NOTE_BEAN"));
  bean.put("insertFiles",new JSONArray().put(image.getPath())).put("insertFileType",0);
  return intent.putExtra("OPEN_NOTE_BEAN",bean.toString());
 }
 JSONObject writeTextResource() throws Exception {
  disableNativeCloud();
  JSONObject row=find(); String id=row.getString("uniqueId");
  if(!id.matches("[0-9a-fA-F-]{36}")) throw new IOException("Unexpected fixture ID");
  File dir=new File(Environment.getExternalStorageDirectory(),".ksync/document/"+id+"/resource/data").getCanonicalFile();
  File[] files=dir.listFiles(); if(files==null) throw new IOException("No fixture resources");
  File target=null; JSONObject content=null;
  for(File file:files) {
   if(!file.getName().endsWith(".html")) continue;
   if(!file.getCanonicalFile().getParentFile().equals(dir) || file.length()>1048576) throw new IOException("Unexpected fixture resource");
   JSONObject candidate=new JSONObject(new String(java.nio.file.Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));
   if(candidate.optInt("docVersion")==2) {
    if(target!=null) throw new IOException("Ambiguous editor resource; refusing write");
    target=file;content=candidate;
   }
  }
  if(target==null) throw new IOException("No current native editor resource");
  String marker=prefs.getString("writeMarker",null);
  if(marker==null) {
   marker="PowerSync inbound "+UUID.randomUUID();
   ReportFiles.write(context,"fixture-resource-before.json",new JSONObject().put("documentId",id).put("path",target.getPath()).put("content",content));
   if(!prefs.edit().putString("writeMarker",marker).putString("writeResource",target.getPath()).commit()) throw new IOException("Cannot journal resource write");
  } else if(!target.getPath().equals(prefs.getString("writeResource",""))) {
   throw new IOException("Native resource changed since journal; inspect before retry");
  }
  JSONObject text=new JSONObject().put("type","text").put("text",marker);
  JSONObject paragraph=new JSONObject().put("type","paragraph").put("content",new JSONArray().put(text));
  content.put("content",new JSONObject().put("type","doc").put("content",new JSONArray().put(paragraph)));
  content.put("currentPos",1);
  android.util.AtomicFile atomic=new android.util.AtomicFile(target);
  FileOutputStream out=atomic.startWrite();
  try {out.write(content.toString().getBytes(StandardCharsets.UTF_8));atomic.finishWrite(out);}
  catch(Exception e) {atomic.failWrite(out);throw e;}
  notes.notifyFixtureData(id);
  return new JSONObject().put("documentId",id).put("resource",target.getName()).put("marker",marker)
   .put("sameIdentity",id.equals(find().getString("uniqueId"))).put("nativeEditorVerified",false);
 }
 JSONObject stageInkPayload() throws Exception {return stageInkPayload(false);}
 JSONObject stageInkPayload(boolean interruptAfterPoints) throws Exception {
  disableNativeCloud();
  JSONObject row=find();String id=row.getString("uniqueId");
  if(!id.matches("[0-9a-fA-F-]{32,36}")) throw new IOException("Unexpected fixture ID");
  File exchange=new File(Environment.getExternalStorageDirectory(),"PowerSyncProbe/ink-inbound.zip");
  byte[] received;
  try(InputStream in=new FileInputStream(exchange)) {received=NativeInk.read(in);}
  String receipt=NativeInk.digest(received);
  File inbox=new File(context.getFilesDir(),"ink-inbox");
  if(!inbox.isDirectory() && !inbox.mkdirs()) throw new IOException("Cannot create private inbox");
  File source=new File(inbox,receipt+".zip");
  if(!source.exists()) {
   android.util.AtomicFile saved=new android.util.AtomicFile(source);FileOutputStream out=saved.startWrite();
   try {out.write(received);saved.finishWrite(out);}catch(Exception e){saved.failWrite(out);throw e;}
  } else if(!receipt.equals(NativeInk.digest(java.nio.file.Files.readAllBytes(source.toPath())))) throw new IOException("Private receipt integrity mismatch");
  String journalName="ink-apply-"+receipt+".json";
  JSONObject manifest=NativeArchive.inspect(source);
  File root=new File(Environment.getExternalStorageDirectory(),".ksync").getCanonicalFile();
  java.util.ArrayList<String> paths=new java.util.ArrayList<>();
  try(java.util.zip.ZipFile zip=new java.util.zip.ZipFile(source)) {
   java.util.Enumeration<? extends java.util.zip.ZipEntry> entries=zip.entries();
   while(entries.hasMoreElements()) {
    java.util.zip.ZipEntry entry=entries.nextElement();String path=entry.getName();
    if(entry.isDirectory()) continue;
    String quoted=java.util.regex.Pattern.quote(id);
    if(!path.matches("document/"+quoted+"/shape/[0-9a-fA-F-]{32,36}#[0-9a-fA-F-]{32,36}#[0-9]+\\.zip")
      && !path.matches("document/"+quoted+"/resource/data/[0-9a-fA-F-]{32,36}\\.png")
      && !path.matches("document/"+quoted+"/resource/pb/[0-9a-fA-F-]{32,36}#[0-9]+")
      && !path.matches("point/"+quoted+"/[0-9a-fA-F-]{32,36}/[0-9a-fA-F-]{32,36}#[0-9a-fA-F-]{32,36}#points")) throw new IOException("Payload outside ink fixture scope");

    paths.add(path);
   }
   if(paths.size()!=2 && paths.size()!=4) throw new IOException("Lab payload requires exactly one shape revision and point file");
   String shapePath=paths.stream().filter(path->path.contains("/shape/")).findFirst().orElseThrow();
   String pointPath=paths.stream().filter(path->path.startsWith("point/")).findFirst().orElseThrow();
   String page=shapePath.substring(shapePath.lastIndexOf('/')+1).split("#")[0];
   String pointPage=pointPath.substring(pointPath.lastIndexOf('/')+1).split("#")[0];
   String pointRevision=pointPath.substring(pointPath.lastIndexOf('/')+1).split("#")[1];
   if(!page.equals(pointPage) || !pointPath.split("/")[2].equals(page)) throw new IOException("Payload page mismatch");
   JSONArray nativePages=notes.pages(id).getJSONArray("pageIds");boolean mapped=false;
   for(int i=0;i<nativePages.length();i++) if(page.equals(nativePages.getString(i))) mapped=true;
   if(!mapped) throw new IOException("Payload page is not mapped to ink fixture");
   try(InputStream shapes=zip.getInputStream(zip.getEntry(shapePath));InputStream points=zip.getInputStream(zip.getEntry(pointPath))) {
    NativeInk.validate(NativeInk.read(shapes),NativeInk.read(points),page,pointRevision);
   }
   if(paths.size()==4) {
    String assetPath=paths.stream().filter(path->path.contains("/resource/data/")).findFirst().orElseThrow();
    String resourcePath=paths.stream().filter(path->path.contains("/resource/pb/")).findFirst().orElseThrow();
    byte[] shapeBytes,resourceBytes,imageBytes;
    try(InputStream in=zip.getInputStream(zip.getEntry(shapePath))) {shapeBytes=NativeInk.read(in);}
    try(InputStream in=zip.getInputStream(zip.getEntry(resourcePath))) {resourceBytes=NativeInk.read(in);}
    try(InputStream in=zip.getInputStream(zip.getEntry(assetPath))) {imageBytes=NativeInk.read(in);}
    java.util.Map<Integer,Object> shape;
    try(java.util.zip.ZipInputStream nested=new java.util.zip.ZipInputStream(new ByteArrayInputStream(shapeBytes))) {
     if(nested.getNextEntry()==null) throw new IOException("Missing image shape");
     shape=NativeInk.fields((byte[])NativeInk.fields(NativeInk.read(nested)).get(1));
    }
    if(!Long.valueOf(19).equals(shape.get(12))) throw new IOException("Image resources require native image shape");
    JSONObject ref=new JSONObject(new String((byte[])shape.get(14),StandardCharsets.UTF_8));
    java.util.Map<Integer,Object> resource=NativeInk.fields((byte[])NativeInk.fields(resourceBytes).get(1));
    String resourceId=new String((byte[])resource.get(1),StandardCharsets.UTF_8);
    String resourceDoc=new String((byte[])resource.get(2),StandardCharsets.UTF_8);
    String relative=new String((byte[])resource.get(8),StandardCharsets.UTF_8);
    String resourceRevision=new String((byte[])resource.get(11),StandardCharsets.UTF_8);
    String expectedRevision=resourcePath.substring(resourcePath.lastIndexOf('/')+1).split("#")[0];
    if(!id.equals(resourceDoc) || !id.equals(ref.getString("documentId")) || !resourceId.equals(ref.getString("uniqueId"))
      || !relative.equals(ref.getString("relativePath")) || !assetPath.equals("document/"+id+"/resource/data"+relative)
      || !resourceRevision.equals(expectedRevision) || !resourceRevision.equals(ref.getString("revisionId"))) throw new IOException("Image resource references mismatch");
    android.graphics.BitmapFactory.Options dimensions=new android.graphics.BitmapFactory.Options();dimensions.inJustDecodeBounds=true;
    android.graphics.BitmapFactory.decodeByteArray(imageBytes,0,imageBytes.length,dimensions);
    if(dimensions.outWidth<1 || dimensions.outHeight<1 || dimensions.outWidth>2048 || dimensions.outHeight>2048) throw new IOException("Invalid fixture image dimensions");
   }
   // A retry may find some or all immutable files already written.
   for(String path:paths) {
    File target=new File(root,path).getCanonicalFile();
    if(!target.toPath().startsWith(root.toPath())) throw new IOException("Unexpected payload target");
    if(target.exists()) {
     try(InputStream actual=new FileInputStream(target);InputStream expected=zip.getInputStream(zip.getEntry(path))) {
      if(!java.util.Arrays.equals(NativeInk.read(actual),NativeInk.read(expected))) throw new IOException("Conflicting bytes for immutable native revision");
     }
    }
   }
   // Points must be durable before publishing the revision that references them.
   paths.sort(java.util.Comparator.comparingInt(path->path.contains("/resource/data/")?0:path.contains("/resource/pb/")?1:path.startsWith("point/")?2:3));
   JSONObject journal=new JSONObject().put("documentId",id).put("manifest",manifest).put("paths",new JSONArray(paths)).put("receipt",receipt).put("phase","prepared");
   ReportFiles.write(context,journalName,journal);
   for(String path:paths) {
    File target=new File(root,path).getCanonicalFile();
    if(!target.toPath().startsWith(root.toPath())) throw new IOException("Unexpected payload target");
    if(target.exists()) continue;
    if(!target.getParentFile().isDirectory() && !target.getParentFile().mkdirs()) throw new IOException("Cannot create native payload directory");
    android.util.AtomicFile atomic=new android.util.AtomicFile(target);FileOutputStream out=atomic.startWrite();
    try(InputStream in=zip.getInputStream(zip.getEntry(path))) {
     byte[] buffer=new byte[8192];int n;long count=0;
     while((n=in.read(buffer))!=-1) {count+=n;if(count>1048576) throw new IOException("Fixture payload exceeds limit");out.write(buffer,0,n);}
     atomic.finishWrite(out);
    } catch(Exception e) {atomic.failWrite(out);throw e;}
    if(interruptAfterPoints && path.startsWith("point/")) {
     ReportFiles.write(context,journalName,journal.put("phase","points-staged"));
     throw new IOException("Injected interruption after durable point staging");
    }
   }
   ReportFiles.write(context,journalName,journal.put("phase","staged"));
   notes.notifyFixtureData(id);
   ReportFiles.write(context,journalName,journal.put("phase","notified"));
  }
  return new JSONObject().put("documentId",id).put("staged",true).put("sameIdentity",id.equals(find().getString("uniqueId"))).put("nativeEditorVerified",false);
 }
 JSONObject capture() throws Exception {
  JSONObject row=find(); String id=row.getString("uniqueId");
  JSONObject report=new JSONObject().put("completeNativeSnapshot",false).put("note",row).put("pages",notes.pages(id));
  // Provider routing for per-document shapes is global; never pretend an arbitrary open DB belongs to this fixture.
  return report;
 }
}
