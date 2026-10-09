package dev.aragonite.powersync;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONObject;

/** Native reader qualification restricted to one hash-verified, authored EPUB. */
final class NativeReaderLab {
 private static final String BOOK="PowerSync-reader-fixture.epub";
 private static final String DIGEST="c75e1ac3d91d1082b4597a3fb100eee03f2681d17976066595c030c8eecc5683";
 private final Context context;
 NativeReaderLab(Context context){this.context=context;}
 private static String read(File file) throws IOException {if(file.length()>4194304)throw new IOException("Reader journal exceeds bound");return new String(java.nio.file.Files.readAllBytes(file.toPath()),java.nio.charset.StandardCharsets.UTF_8);}
 JSONObject libraryGuard() throws Exception {
  JSONArray rows=new NativeNotes(context).rows(Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata"),"nocasePath NOT IN (?,?,?,?) OR nocasePath IS NULL",new String[]{"/storage/emulated/0/Download/"+BOOK,"/sdcard/Download/"+BOOK,"/storage/emulated/0/Download/PowerSync-reader-geometry.pdf","/sdcard/Download/PowerSync-reader-geometry.pdf"},true);
  if(rows.length()>20000)throw new IOException("Reader guard exceeds bound");
  java.util.ArrayList<String> signatures=new java.util.ArrayList<>();
  for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);signatures.add(row.optString("uuid")+":"+row.optString("idString")+":"+row.optInt("status")+":"+row.optInt("userDataSyncStatus")+":"+row.optInt("fileSyncStatus"));}
  ReportFiles.write(context,"reader-library-guard-private.json",new JSONObject().put("rows",rows).put("signatures",new JSONArray(signatures)));
  java.util.Collections.sort(signatures);String digest=NativeInk.digest(String.join("\n",signatures).getBytes(java.nio.charset.StandardCharsets.UTF_8));
  File file=new File(context.getFilesDir(),"reader-library-guard-baseline.json");if(!file.exists())ReportFiles.write(context,file.getName(),new JSONObject().put("digest",digest).put("rows",rows.length()));
  JSONObject baseline=new JSONObject(read(file));return new JSONObject().put("originalReaderRows",rows.length()).put("originalReaderIdentityStatusFlagsPreserved",baseline.getString("digest").equals(digest));
 }
 JSONObject request(boolean start) throws Exception {return request(start?"start":"metadata");}
 JSONObject request(String operation) throws Exception {
  boolean start="start".equals(operation),bookmarkPublication="bookmarks".equals(operation),annotationPublication="annotations".equals(operation);
  if(!start&&!bookmarkPublication&&!annotationPublication&&!"metadata".equals(operation))throw new IOException("Unknown reader operation");
  NativeSystemConfigProbe io=new NativeSystemConfigProbe();
  if(!"powersync_lab".equals(io.read().getString("curClusterId")))throw new IOException("Reader publication requires lab endpoint");
  capture();
  JSONObject captured=new JSONObject(read(new File(context.getFilesDir(),"reader-fixture-private.json")));
  JSONArray rows=captured.getJSONArray("metadata");
  File baseline=new File(context.getFilesDir(),"reader-fixture-sync-baseline.json");
  String selected=baseline.exists()?new JSONObject(read(baseline)).getString("uuid"):null;
  File selection=new File(context.getFilesDir(),"reader-publication-selection.json");
  if(selection.exists()){
   if(!baseline.exists())throw new IOException("Explicit alias selection requires original identity baseline");
   selected=new JSONObject(read(selection)).getString("uuid");
  }
  if(selected==null && rows.length()!=1)throw new IOException("Initial fixture identity must be unambiguous");
  JSONObject row=null;for(int i=0;i<rows.length();i++){JSONObject candidate=rows.getJSONObject(i);if(selected==null||selected.equals(candidate.getString("uuid"))){if(row!=null)throw new IOException("Duplicate selected fixture UUID");row=candidate;}}
  if(row==null)throw new IOException("Original fixture UUID is missing");String id=row.getString("uuid");
  JSONArray allowed=new JSONObject(read(new File(context.getFilesDir(),"reader-sync-scope.json"))).getJSONArray("bookIds");
  boolean admitted=false;for(int i=0;i<allowed.length();i++){String value=allowed.getString(i);if(!value.matches("[0-9a-f-]{32,36}"))throw new IOException("Invalid reader scope");if(id.equals(value))admitted=true;}
  if(!admitted || allowed.length()>8)throw new IOException("Unenrolled fixture identity");
  if(!baseline.exists())ReportFiles.write(context,baseline.getName(),new JSONObject().put("uuid",id).put("userDataSyncStatus",row.getInt("userDataSyncStatus")).put("fileSyncStatus",row.getInt("fileSyncStatus")));
  android.content.ContentValues values=new android.content.ContentValues();values.put("userDataSyncStatus",1);
  int updated=context.getContentResolver().update(Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata"),values,"uuid=? AND nocasePath=?",new String[]{id,row.getString("nocasePath")});
  if(updated!=1)throw new IOException("Scoped reader enable did not update one row");
  JSONObject system=io.read("ONYX_SYSTEM_CONFIG_KEY");system.getJSONObject("configMap").put("key_enable_sync_reader_data","true").put("key_enable_sync_note_data","false");io.write("ONYX_SYSTEM_CONFIG_KEY",system);
  context.sendBroadcast(new android.content.Intent("com.onyx.action.system.config.changed"));
  row.put("uniqueId",id).put("user","powersync-lab").put("dbId","powersync-lab-READER_LIBRARY").put("modeType",4).put("userDataSyncStatus",1);
  JSONObject content=start?new JSONObject().put("startAllDoc",false).put("startDocIdList",allowed):new JSONObject().put("metadataModelList",new JSONArray().put(row)).put("libraryModelList",new JSONArray()).put("collectionModelList",new JSONArray()).put("requestType","SYNC_DOCUMENT").put("isSyncUserOtherData",false);
  if(bookmarkPublication){
   JSONArray nativeBookmarks=captured.getJSONArray("bookmarks"),models=new JSONArray();
   if(nativeBookmarks.length()==0)throw new IOException("Create a native fixture bookmark before publication");
   for(int i=0;i<nativeBookmarks.length();i++){
    JSONObject bookmark=new JSONObject(nativeBookmarks.getJSONObject(i).toString());
    if(!id.equals(bookmark.getString("idString")))continue;
    bookmark.put("uniqueId",bookmark.getString("uuid")).put("documentId",id).put("modeType",2);models.put(bookmark);
   }
   if(models.length()==0)throw new IOException("Selected native fixture has no bookmarks");
   content=new JSONObject().put("bookmarkModelList",models).put("annotationModelList",new JSONArray()).put("noteDocumentModelList",new JSONArray());
  }
  if(annotationPublication){
   JSONArray annotations=captured.getJSONArray("annotations"),models=new JSONArray();
   String[] fields={"createdAt","updatedAt","quote","startPosition","endPosition","startPositionInt","endPositionInt","note","linkNote","application","position","positionV2","positionV3","pageNumber","oldStartPosition","oldEndPosition","positionType","color","shape","chapter","status","pageXPath","startXPath","endXPath","positionVersion"};
   for(int i=0;i<annotations.length();i++){
    JSONObject annotation=annotations.getJSONObject(i);if(!id.equals(annotation.getString("idString")))continue;
    JSONObject model=new JSONObject();for(String field:fields)if(annotation.has(field))model.put(field,annotation.get(field));
    model.put("uniqueId",annotation.getString("uuid")).put("documentId",id).put("modeType",1);
    // Native SQLite stores rectangles as JSON; the sync model needs the array.
    Object rectangles=annotation.opt("rectangles");
    model.put("rectangles",rectangles instanceof String?new JSONArray((String)rectangles):rectangles==null||rectangles==JSONObject.NULL?new JSONArray():rectangles);
    models.put(model);
   }
   if(models.length()==0)throw new IOException("Selected native fixture has no annotations");
   content=new JSONObject().put("bookmarkModelList",new JSONArray()).put("annotationModelList",models).put("noteDocumentModelList",new JSONArray());
  }
  if(!start)ReportFiles.write(context,"reader-publication-request.json",content);
  try(android.os.ParcelFileDescriptor payload=start?null:android.os.ParcelFileDescriptor.open(new File(context.getFilesDir(),"reader-publication-request.json"),android.os.ParcelFileDescriptor.MODE_READ_ONLY)){
  try(RemoteBinding binding=new RemoteBinding(context,"com.onyx.android.ksync","com.onyx.android.ksync.service.KSyncService")){
   android.os.IBinder binder=binding.await();String descriptor="com.onyx.android.sdk.sync.ksync.service.ISyncService";if(!descriptor.equals(binder.getInterfaceDescriptor()))throw new IOException("Unexpected reader Binder");
   android.os.Parcel p=android.os.Parcel.obtain();try{
    p.writeInterfaceToken(descriptor);p.writeInt(1);p.writeString("com.onyx.android.sdk.sync.ksync.KSyncContext");p.writeString("dev.aragonite.powersync");p.writeString("powersync-lab");p.writeString(id);p.writeString(null);
    p.writeString(start?content.toString():null);p.writeParcelable(payload,0);p.writeInt(2);p.writeByte((byte)0);p.writeLong(0);p.writeInt(0);p.writeString(null);p.writeString(null);p.writeByte((byte)0);p.writeInt(0);p.writeByte((byte)0);p.writeLong(0);p.writeString(null);p.writeInt(0);p.writeString(null);p.writeStrongBinder(null);
    if(!binder.transact(start?17:((bookmarkPublication||annotationPublication)?3:1),p,null,android.os.IBinder.FLAG_ONEWAY))throw new IOException("Reader transaction rejected");
   }finally{p.recycle();}
  }
  }
  return new JSONObject().put("explicitAliasSelection",selection.exists()).put("requested",true).put("completed",false).put("operation",start?"scoped-start":(bookmarkPublication?"fixture-bookmark-publication":annotationPublication?"fixture-annotation-publication":"single-book-metadata-publication"));
 }
 JSONObject pullFullUserData() throws Exception {return pullFullUserData(false);}
 JSONObject pullFullUserData(boolean metadataOnly) throws Exception {
  if(!"powersync_lab".equals(new NativeSystemConfigProbe().read().getString("curClusterId")))throw new IOException("Reader query requires lab endpoint");
  String id=new JSONObject(read(new File(context.getFilesDir(),"reader-fixture-sync-baseline.json"))).getString("uuid");
  File selection=new File(context.getFilesDir(),"reader-publication-selection.json");if(selection.exists())id=new JSONObject(read(selection)).getString("uuid");
  JSONArray allowed=new JSONObject(read(new File(context.getFilesDir(),"reader-sync-scope.json"))).getJSONArray("bookIds");boolean admitted=false,present=false;
  for(int i=0;i<allowed.length();i++)if(id.equals(allowed.getString(i)))admitted=true;
  File managed=new File("/storage/emulated/0/.ksync/powersync-lab/reader/"+id+"/book/"+id+".epub");JSONArray metadata;
  if(managed.isFile()&&managed.length()<=1048576&&DIGEST.equals(NativeInk.digest(java.nio.file.Files.readAllBytes(managed.toPath()))))metadata=new NativeNotes(context).rows(Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata"),"uuid=?",new String[]{id},true);
  else {capture();metadata=new JSONObject(read(new File(context.getFilesDir(),"reader-fixture-private.json"))).getJSONArray("metadata");}
for(int i=0;i<metadata.length();i++)if(id.equals(metadata.getJSONObject(i).getString("uuid")))present=true;
  if(!admitted||(!present&&!metadataOnly)|| (metadataOnly&&(!managed.isFile()||managed.length()>1048576||!DIGEST.equals(NativeInk.digest(java.nio.file.Files.readAllBytes(managed.toPath()))))))throw new IOException("Query identity is outside verified fixture scope");
  final String requested=id;java.util.concurrent.CompletableFuture<JSONObject> result=new java.util.concurrent.CompletableFuture<>();
  android.os.Binder callback=new android.os.Binder(){
   @Override protected boolean onTransact(int code,android.os.Parcel data,android.os.Parcel reply,int flags) throws android.os.RemoteException {
    String descriptor="com.onyx.android.sdk.sync.ksync.service.SyncIntentCallback";
    if(code==android.os.IBinder.INTERFACE_TRANSACTION){if(reply!=null)reply.writeString(descriptor);return true;}
    if(code!=1)return super.onTransact(code,data,reply,flags);
    try{
     data.enforceInterface(descriptor);if(data.readInt()==0)throw new IOException("Empty reader callback");
     if(!"com.onyx.android.sdk.sync.ksync.KSyncContext".equals(data.readString()))throw new IOException("Unexpected reader context");
     data.readString();String user=data.readString(),book=data.readString();data.readString();
     if(!"powersync-lab".equals(user)||!requested.equals(book))throw new IOException("Reader callback identity mismatch");
     data.readString();android.os.ParcelFileDescriptor payload=data.readParcelable(android.os.ParcelFileDescriptor.class.getClassLoader());
     int status=data.readInt();boolean completed=data.readByte()!=0;data.readLong();int offset=data.readInt();data.readString();data.readString();data.readByte();data.readInt();data.readByte();long total=data.readLong();data.readString();data.readInt();data.readString();
     if(status!=2||payload==null)throw new IOException("Reader query failed: "+status);
     JSONObject body;
     try(java.io.InputStream stream=new android.os.ParcelFileDescriptor.AutoCloseInputStream(payload)){
      java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;
      while((count=stream.read(buffer))!=-1){if(bytes.size()+count>4194304)throw new IOException("Reader query exceeds bound");bytes.write(buffer,0,count);}
      body=new JSONObject(new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8));
     }
     ReportFiles.write(context,"reader-full-userdata-private.json",body);
     result.complete(new JSONObject().put("readOnly",true).put("callbackStatus",status).put("completed",completed).put("offset",offset).put("totalCount",total).put("metadataModels",body.optJSONArray("metadataModelList")==null?0:body.getJSONArray("metadataModelList").length()).put("bookmarkModels",body.optJSONArray("bookmarkModelList")==null?0:body.getJSONArray("bookmarkModelList").length()));
    }catch(Exception e){result.completeExceptionally(e);}
    if(reply!=null)reply.writeNoException();return true;
   }
  };
  try(RemoteBinding binding=new RemoteBinding(context,"com.onyx.android.ksync","com.onyx.android.ksync.service.KSyncService")){
   android.os.IBinder binder=binding.await();String descriptor="com.onyx.android.sdk.sync.ksync.service.ISyncService";
   if(!descriptor.equals(binder.getInterfaceDescriptor()))throw new IOException("Unexpected reader Binder");
   android.os.Parcel parcel=android.os.Parcel.obtain();try{writeArgs(parcel,id,metadataOnly?new JSONObject().put("documentId",id).toString():null,null,callback);if(!binder.transact(metadataOnly?5:11,parcel,null,android.os.IBinder.FLAG_ONEWAY))throw new IOException("Reader query rejected");}finally{parcel.recycle();}
   return result.get(35,java.util.concurrent.TimeUnit.SECONDS);
  }
 }
 private void writeArgs(android.os.Parcel p,String id,String content,android.os.ParcelFileDescriptor payload,android.os.IBinder callback){
  p.writeInterfaceToken("com.onyx.android.sdk.sync.ksync.service.ISyncService");p.writeInt(1);p.writeString("com.onyx.android.sdk.sync.ksync.KSyncContext");p.writeString("dev.aragonite.powersync");p.writeString("powersync-lab");p.writeString(id);p.writeString(null);
  p.writeString(content);p.writeParcelable(payload,0);p.writeInt(2);p.writeByte((byte)0);p.writeLong(0);p.writeInt(0);p.writeString(null);p.writeString(null);p.writeByte((byte)0);p.writeInt(0);p.writeByte((byte)0);p.writeLong(0);p.writeString(null);p.writeInt(0);p.writeString(null);p.writeStrongBinder(callback);
 }
 JSONObject syncBookFromLibrary() throws Exception {return syncBookFromLibrary(false);}
 JSONObject syncBookFromLibrary(boolean libraryDelta) throws Exception {
  if(!"powersync_lab".equals(new NativeSystemConfigProbe().read().getString("curClusterId")))throw new IOException("Library request requires lab endpoint");
  File escrow=new File(context.getFilesDir(),"reader-book-escrow.epub");
  if(!escrow.isFile()||escrow.length()>1048576||!DIGEST.equals(NativeInk.digest(java.nio.file.Files.readAllBytes(escrow.toPath()))))throw new IOException("Authored book escrow required");
  String id=new JSONObject(read(new File(context.getFilesDir(),"reader-publication-selection.json"))).getString("uuid");
  JSONArray allowed=new JSONObject(read(new File(context.getFilesDir(),"reader-sync-scope.json"))).getJSONArray("bookIds");boolean admitted=false;
  for(int i=0;i<allowed.length();i++)if(id.equals(allowed.getString(i)))admitted=true;
  if(!admitted||!id.matches("[0-9a-f-]{32,36}"))throw new IOException("Library request outside fixture scope");
  String path="/storage/emulated/0/.ksync/powersync-lab/reader/"+id+"/book/"+id+".epub";
  File book=new File(path);if(!book.isFile()||book.length()!=escrow.length()||!DIGEST.equals(NativeInk.digest(java.nio.file.Files.readAllBytes(book.toPath()))))throw new IOException("Exact managed fixture download required");
  java.util.concurrent.CompletableFuture<JSONObject> completed=new java.util.concurrent.CompletableFuture<>();
  android.os.Binder callback=new android.os.Binder(){
   @Override protected boolean onTransact(int code,android.os.Parcel data,android.os.Parcel reply,int flags) throws android.os.RemoteException {
    String descriptor="com.onyx.android.sdk.sync.ksync.reader.client.service.ReaderSyncIntentCallback";
    if(code==android.os.IBinder.INTERFACE_TRANSACTION){if(reply!=null)reply.writeString(descriptor);return true;}
    if(code!=1)return super.onTransact(code,data,reply,flags);
    try{data.enforceInterface(descriptor);if(data.readInt()==0)throw new IOException("Empty library callback");
     String receivedPath=data.readString(),receivedId=data.readString();boolean needPull=data.readByte()!=0,needPush=data.readByte()!=0;android.os.ParcelFileDescriptor payload=data.readParcelable(android.os.ParcelFileDescriptor.class.getClassLoader());int type=data.readInt(),result=data.readInt();String message=data.readString();boolean newData=data.readByte()!=0;String toId=data.readString();
     if(payload!=null)payload.close();if(!path.equals(receivedPath)||!id.equals(receivedId)||type!=10002)throw new IOException("Library callback scope mismatch");
     ReportFiles.write(context,"reader-library-callback-private.json",new JSONObject().put("path",receivedPath).put("fromId",receivedId).put("toId",toId).put("message",message).put("result",result));
     completed.complete(new JSONObject().put("requested",true).put("callbackReceived",true).put("nativeResult",result).put("newAnnotationDataPulled",newData).put("identityChanged",toId!=null&&!id.equals(toId)).put("pushRequested",needPush).put("pullRequested",needPull));
    }catch(Exception e){completed.completeExceptionally(e);}if(reply!=null)reply.writeNoException();return true;
   }
  };
  try(RemoteBinding binding=new RemoteBinding(context,"com.onyx.kreader","com.onyx.android.sdk.readerview.service.ReaderSyncService")){
   android.os.IBinder binder=binding.await();String descriptor="com.onyx.android.sdk.sync.ksync.reader.client.service.IReaderSyncService";
   if(!descriptor.equals(binder.getInterfaceDescriptor()))throw new IOException("Unexpected native reader Binder");
   if(libraryDelta){android.os.Parcel request=android.os.Parcel.obtain();try{request.writeInterfaceToken(descriptor);if(!binder.transact(2,request,null,android.os.IBinder.FLAG_ONEWAY))throw new IOException("Native library delta rejected");}finally{request.recycle();}return new JSONObject().put("requested",true).put("completed",false).put("wholeNativeLibraryDelta",true);}

   android.os.Parcel parcel=android.os.Parcel.obtain();try{
    parcel.writeInterfaceToken(descriptor);parcel.writeInt(1);parcel.writeString(path);parcel.writeString(id);parcel.writeByte((byte)1);parcel.writeByte((byte)0);parcel.writeParcelable(null,0);parcel.writeInt(10002);parcel.writeInt(0);parcel.writeString(null);parcel.writeByte((byte)0);parcel.writeString(null);parcel.writeStrongBinder(callback);
    if(!binder.transact(3,parcel,null,android.os.IBinder.FLAG_ONEWAY))throw new IOException("Native library request rejected");
   }finally{parcel.recycle();}
  }
  return completed.get(40,java.util.concurrent.TimeUnit.SECONDS);
 }
 JSONObject startScopedCloudLibraryReplicator() throws Exception {return startScopedCloudLibraryReplicator(false);}
 JSONObject startScopedCloudLibraryReplicator(boolean restart) throws Exception {
  if(!"powersync_lab".equals(new NativeSystemConfigProbe().read().getString("curClusterId")))throw new IOException("Cloud replicator requires lab endpoint");
  String id=new JSONObject(read(new File(context.getFilesDir(),"reader-publication-selection.json"))).getString("uuid");JSONArray ids=new JSONObject(read(new File(context.getFilesDir(),"reader-sync-scope.json"))).getJSONArray("bookIds");boolean admitted=false;
  if(ids.length()==0||ids.length()>8)throw new IOException("Invalid cloud scope bound");for(int i=0;i<ids.length();i++){String value=ids.getString(i);if(!value.matches("[0-9a-f-]{32,36}"))throw new IOException("Invalid cloud scope UUID");if(id.equals(value))admitted=true;}
  if(!admitted)throw new IOException("Unenrolled cloud UUID");File book=new File("/storage/emulated/0/.ksync/powersync-lab/reader/"+id+"/book/"+id+".epub");if(!book.isFile()||book.length()>1048576||!DIGEST.equals(NativeInk.digest(java.nio.file.Files.readAllBytes(book.toPath()))))throw new IOException("Verified managed cloud EPUB required");
  String content=new JSONObject().put("startAllDoc",false).put("startDocIdList",ids).toString();
  try(RemoteBinding binding=new RemoteBinding(context,"com.onyx.android.ksync","com.onyx.android.ksync.service.KSyncService")){
   android.os.IBinder binder=binding.await();if(!"com.onyx.android.sdk.sync.ksync.service.ISyncService".equals(binder.getInterfaceDescriptor()))throw new IOException("Unexpected cloud replicator Binder");android.os.Parcel p=android.os.Parcel.obtain();try{writeArgs(p,id,content,null,null);if(!binder.transact(restart?16:17,p,null,android.os.IBinder.FLAG_ONEWAY))throw new IOException("Cloud library replicator request rejected");}finally{p.recycle();}
  }
  return new JSONObject().put("requested",true).put("completed",false).put("scopeCount",ids.length()).put("replicator","READER_LIBRARY").put("restartRequested",restart).put("bookDataReplicatorStartRequested",false).put("readerImportRequested",false);
 }
 JSONObject captureCloudBook() throws Exception {
  if(!"powersync_lab".equals(new NativeSystemConfigProbe().read().getString("curClusterId")))throw new IOException("Cloud book capture requires lab endpoint");
  String id=new JSONObject(read(new File(context.getFilesDir(),"reader-publication-selection.json"))).getString("uuid");JSONArray scope=new JSONObject(read(new File(context.getFilesDir(),"reader-sync-scope.json"))).getJSONArray("bookIds");boolean allowed=false;for(int i=0;i<scope.length();i++)if(id.equals(scope.getString(i)))allowed=true;
  if(!allowed||!id.matches("[0-9a-f-]{32,36}"))throw new IOException("Cloud book outside fixture scope");String path="/storage/emulated/0/.ksync/powersync-lab/reader/"+id+"/book/"+id+".epub";File file=new File(path);
  if(!file.isFile()||file.length()>1048576||!DIGEST.equals(NativeInk.digest(java.nio.file.Files.readAllBytes(file.toPath()))))throw new IOException("Exact managed authored EPUB required");
  NativeNotes notes=new NativeNotes(context);JSONArray metadata=notes.rows(Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata"),"uuid=? AND nocasePath=?",new String[]{id,path},true);
  if(metadata.length()!=1)throw new IOException("Cloud UUID/path must identify one native book");JSONArray bookmarks=notes.rows(Uri.parse("content://com.onyx.content.database.ContentProvider/Bookmark"),"idString=?",new String[]{id},true);JSONArray annotations=notes.rows(Uri.parse("content://com.onyx.content.database.ContentProvider/Annotation"),"idString=?",new String[]{id},true);
  if(bookmarks.length()>128||annotations.length()>128)throw new IOException("Cloud fixture model bound exceeded");ReportFiles.write(context,"reader-cloud-book-private.json",new JSONObject().put("metadata",metadata).put("bookmarks",bookmarks).put("annotations",annotations));
  return new JSONObject().put("readOnly",true).put("cloudIdentityAndManagedPathMatched",true).put("bookmarkRows",bookmarks.length()).put("annotationRows",annotations.length());
 }
 JSONObject editCloudFixtureComment() throws Exception {
  captureCloudBook();
  JSONObject before=new JSONObject(read(new File(context.getFilesDir(),"reader-cloud-book-private.json")));
  JSONArray annotations=before.getJSONArray("annotations");
  if(annotations.length()!=1)throw new IOException("Local-comment test requires exactly one fixture annotation");
  JSONObject annotation=annotations.getJSONObject(0);
  String book=before.getJSONArray("metadata").getJSONObject(0).getString("uuid"),id=annotation.getString("uuid");
  if(!book.equals(annotation.getString("idString"))||annotation.getInt("status")!=0)throw new IOException("Local-comment test requires an enabled enrolled annotation");
  long timestamp=Math.max(System.currentTimeMillis(),annotation.getLong("updatedAt")+1);
  ReportFiles.write(context,"reader-cloud-comment-before-private.json",before);
  android.content.ContentValues values=new android.content.ContentValues();values.put("note","PowerSync newer local comment");values.put("updatedAt",timestamp);
  int changed=context.getContentResolver().update(Uri.parse("content://com.onyx.content.database.ContentProvider/Annotation"),values,"uuid=? AND idString=?",new String[]{id,book});
  if(changed!=1)throw new IOException("Local-comment test must update exactly one fixture annotation");
  captureCloudBook();JSONObject after=new JSONObject(read(new File(context.getFilesDir(),"reader-cloud-book-private.json")));
  JSONObject saved=after.getJSONArray("annotations").getJSONObject(0);
  if(!id.equals(saved.getString("uuid"))||!"PowerSync newer local comment".equals(saved.getString("note"))||saved.getLong("updatedAt")!=timestamp)throw new IOException("Local comment did not persist exactly");
  return new JSONObject().put("updatedFixtureRows",changed).put("unprivilegedLocalCommentSaved",true).put("updatedAt",timestamp);
 }
 private JSONObject selectedConflictRow(JSONArray rows) throws Exception {
  String id=new JSONObject(read(new File(context.getFilesDir(),"reader-conflict-selection.json"))).getString("annotationId");
  if(!id.matches("[0-9a-f-]{32,36}"))throw new IOException("Invalid synthetic annotation UUID");
  JSONObject selected=null;for(int i=0;i<rows.length();i++)if(id.equals(rows.getJSONObject(i).getString("uuid"))){if(selected!=null)throw new IOException("Duplicate conflict UUID");selected=rows.getJSONObject(i);}
  if(selected==null)throw new IOException("Selected conflict annotation absent");return selected;
 }
 JSONObject editCloudConflict() throws Exception {
  captureCloudBook();
  JSONObject before=new JSONObject(read(new File(context.getFilesDir(),"reader-cloud-book-private.json")));
  JSONObject request=new JSONObject(read(new File(context.getFilesDir(),"reader-conflict-edit.json")));
  String note=request.getString("note");int status=request.getInt("status");
  if(!note.matches("PowerSync conflict (palma|lumi) (first|second|edit|delete)")||(status!=0&&status!=1))throw new IOException("Invalid synthetic conflict edit");
  JSONObject row=selectedConflictRow(before.getJSONArray("annotations"));String book=before.getJSONArray("metadata").getJSONObject(0).getString("uuid"),id=row.getString("uuid");
  if(!book.equals(row.getString("idString")))throw new IOException("Conflict parent mismatch");
  long stamp=Math.max(System.currentTimeMillis(),row.getLong("updatedAt")+1);
  ReportFiles.write(context,"reader-conflict-before-private.json",before);
  android.content.ContentValues values=new android.content.ContentValues();values.put("note",note);values.put("status",status);values.put("updatedAt",stamp);
  if(context.getContentResolver().update(Uri.parse("content://com.onyx.content.database.ContentProvider/Annotation"),values,"uuid=? AND idString=?",new String[]{id,book})!=1)throw new IOException("Conflict edit requires exactly one row");
  captureCloudBook();JSONObject saved=selectedConflictRow(new JSONObject(read(new File(context.getFilesDir(),"reader-cloud-book-private.json"))).getJSONArray("annotations"));
  if(!id.equals(saved.getString("uuid"))||!note.equals(saved.getString("note"))||saved.getInt("status")!=status||saved.getLong("updatedAt")!=stamp)throw new IOException("Conflict edit verification failed");
  return new JSONObject().put("updatedFixtureRows",1).put("status",status).put("updatedAt",stamp).put("ordinaryUid",android.os.Process.myUid());
 }
 JSONObject publishCloudConflict() throws Exception {
  captureCloudBook();JSONObject nativeBook=new JSONObject(read(new File(context.getFilesDir(),"reader-cloud-book-private.json")));
  JSONObject row=selectedConflictRow(nativeBook.getJSONArray("annotations")),model=new JSONObject(read(new File(context.getFilesDir(),"reader-conflict-model.json")));
  String book=nativeBook.getJSONArray("metadata").getJSONObject(0).getString("uuid");
  if(model.getInt("modeType")!=1||!book.equals(model.getString("documentId"))||!row.getString("uuid").equals(model.getString("uniqueId"))||!row.getString("note").equals(model.getString("note"))||row.getInt("status")!=model.getInt("status")||row.getLong("updatedAt")!=model.getLong("updatedAt")||model.has("_deleted"))throw new IOException("Conflict payload must match enrolled native row");
  model.remove("_id");model.remove("_rev");
  JSONObject payload=new JSONObject().put("annotationModelList",new JSONArray().put(model)).put("bookmarkModelList",new JSONArray()).put("noteDocumentModelList",new JSONArray());
  ReportFiles.write(context,"reader-conflict-publication-private.json",payload);
  try(android.os.ParcelFileDescriptor fd=android.os.ParcelFileDescriptor.open(new File(context.getFilesDir(),"reader-conflict-publication-private.json"),android.os.ParcelFileDescriptor.MODE_READ_ONLY);RemoteBinding binding=new RemoteBinding(context,"com.onyx.android.ksync","com.onyx.android.ksync.service.KSyncService")){
   android.os.IBinder binder=binding.await();if(!"com.onyx.android.sdk.sync.ksync.service.ISyncService".equals(binder.getInterfaceDescriptor()))throw new IOException("Unexpected conflict Binder");
   android.os.Parcel parcel=android.os.Parcel.obtain();try{writeArgs(parcel,book,null,fd,null);if(!binder.transact(3,parcel,null,android.os.IBinder.FLAG_ONEWAY))throw new IOException("Conflict save rejected");}finally{parcel.recycle();}
  }
  return new JSONObject().put("requested",true).put("completed",false).put("nativeCblSaveRequested",true);
 }
 JSONObject prepareBookTransfer() throws Exception {
  if(!"powersync_lab".equals(new NativeSystemConfigProbe().read().getString("curClusterId")))throw new IOException("Book transfer requires lab endpoint");
  capture();JSONObject captured=new JSONObject(read(new File(context.getFilesDir(),"reader-fixture-private.json")));
  String id=new JSONObject(read(new File(context.getFilesDir(),"reader-publication-selection.json"))).getString("uuid");
  JSONObject row=null;JSONArray rows=captured.getJSONArray("metadata");
  for(int i=0;i<rows.length();i++)if(id.equals(rows.getJSONObject(i).getString("uuid")))row=new JSONObject(rows.getJSONObject(i).toString());
  if(row==null)throw new IOException("Selected book absent from fixture capture");
  File book=new File("/storage/emulated/0/Download/"+BOOK);byte[] bytes=java.nio.file.Files.readAllBytes(book.toPath());
  java.nio.file.Files.write(new File(context.getFilesDir(),"reader-book-escrow.epub").toPath(),bytes);
  row.put("uniqueId",id).put("user","powersync-lab").put("dbId","powersync-lab-READER_LIBRARY").put("modeType",4).put("fileOriginSize",bytes.length);
  ReportFiles.write(context,"reader-book-transfer-request.json",new JSONObject().put("metadataModelList",new JSONArray().put(row)));
  return new JSONObject().put("prepared",true).put("bookSha256",DIGEST).put("bytes",bytes.length);
 }
 JSONObject transferBook(boolean upload) throws Exception {
  if(!"powersync_lab".equals(new NativeSystemConfigProbe().read().getString("curClusterId")))throw new IOException("Book transfer requires lab endpoint");
  File escrow=new File(context.getFilesDir(),"reader-book-escrow.epub");
  if(!escrow.isFile()||escrow.length()>1048576||!DIGEST.equals(NativeInk.digest(java.nio.file.Files.readAllBytes(escrow.toPath()))))throw new IOException("Book escrow must match exact authored EPUB");
  File request=new File(context.getFilesDir(),"reader-book-transfer-request.json");JSONObject body=new JSONObject(read(request));JSONArray models=body.getJSONArray("metadataModelList");
  if(models.length()!=1)throw new IOException("Only one synthetic book transfer allowed");JSONObject model=models.getJSONObject(0);String id=model.getString("uniqueId");
  JSONArray allowed=new JSONObject(read(new File(context.getFilesDir(),"reader-sync-scope.json"))).getJSONArray("bookIds");boolean enrolled=false;
  for(int i=0;i<allowed.length();i++)if(id.equals(allowed.getString(i)))enrolled=true;
  if(!enrolled||!id.matches("[0-9a-f-]{32,36}")||!"powersync-lab".equals(model.getString("user"))||!BOOK.equals(model.getString("name"))||!model.getString("nativeAbsolutePath").equals("/storage/emulated/0/Download/"+BOOK)||model.getLong("fileOriginSize")!=escrow.length())throw new IOException("Book transfer outside authored fixture scope");
  if(upload)capture();
  try(android.os.ParcelFileDescriptor payload=android.os.ParcelFileDescriptor.open(request,android.os.ParcelFileDescriptor.MODE_READ_ONLY);
      RemoteBinding binding=new RemoteBinding(context,"com.onyx.android.ksync","com.onyx.android.ksync.service.KSyncService")){
   android.os.IBinder binder=binding.await();if(!"com.onyx.android.sdk.sync.ksync.service.ISyncService".equals(binder.getInterfaceDescriptor()))throw new IOException("Unexpected book Binder");
   android.os.Parcel parcel=android.os.Parcel.obtain();try{writeArgs(parcel,id,null,payload,null);if(!binder.transact(upload?12:13,parcel,null,android.os.IBinder.FLAG_ONEWAY))throw new IOException("Book transfer rejected");}finally{parcel.recycle();}
  }
  return new JSONObject().put("requested",true).put("completed",false).put("operation",upload?"fixture-book-upload":"fixture-book-download");
 }
 JSONObject refreshBookmark() throws Exception {
  if(!"powersync_lab".equals(new NativeSystemConfigProbe().read().getString("curClusterId")))throw new IOException("Bookmark revision requires lab endpoint");
  capture();JSONObject captured=new JSONObject(read(new File(context.getFilesDir(),"reader-fixture-private.json")));
  String target=new JSONObject(read(new File(context.getFilesDir(),"reader-bookmark-revision-request.json"))).getString("uuid");
  JSONArray bookmarks=captured.getJSONArray("bookmarks"),allowed=new JSONObject(read(new File(context.getFilesDir(),"reader-sync-scope.json"))).getJSONArray("bookIds");JSONObject selected=null;
  for(int i=0;i<bookmarks.length();i++)if(target.equals(bookmarks.getJSONObject(i).getString("uuid"))){if(selected!=null)throw new IOException("Duplicate bookmark identity");selected=bookmarks.getJSONObject(i);}
  if(selected==null)throw new IOException("Bookmark is not attached to verified fixture");boolean enrolled=false;
  for(int i=0;i<allowed.length();i++)if(selected.getString("idString").equals(allowed.getString(i)))enrolled=true;
  if(!enrolled)throw new IOException("Bookmark parent is outside lab scope");
  long timestamp=Math.max(System.currentTimeMillis(),selected.getLong("updatedAt")+1);
  ReportFiles.write(context,"reader-bookmark-revision-before-private.json",selected);
  android.content.ContentValues values=new android.content.ContentValues();values.put("updatedAt",timestamp);
  int changed=context.getContentResolver().update(Uri.parse("content://com.onyx.content.database.ContentProvider/Bookmark"),values,"uuid=? AND idString=?",new String[]{target,selected.getString("idString")});
  if(changed!=1)throw new IOException("Bookmark revision must affect one scoped row");
  return new JSONObject().put("fixtureBookmarkTimestampRefreshed",true).put("changedRows",changed);
 }
 JSONObject reorderIdentity(boolean restore) throws Exception {
  NativeSystemConfigProbe io=new NativeSystemConfigProbe();
  if(!restore&&!"powersync_lab".equals(io.read().getString("curClusterId")))throw new IOException("Identity test requires lab endpoint");
  if(!"false".equals(io.read("ONYX_SYSTEM_CONFIG_KEY").getJSONObject("configMap").getString("key_enable_sync_reader_data")))throw new IOException("Stop reader sync before identity test");
  capture();JSONArray rows=new JSONObject(read(new File(context.getFilesDir(),"reader-fixture-private.json"))).getJSONArray("metadata");
  JSONArray allowed=new JSONObject(read(new File(context.getFilesDir(),"reader-sync-scope.json"))).getJSONArray("bookIds");
  for(int i=0;i<rows.length();i++){boolean enrolled=false;for(int j=0;j<allowed.length();j++)if(rows.getJSONObject(i).getString("uuid").equals(allowed.getString(j)))enrolled=true;if(!enrolled)throw new IOException("Identity alias outside enrolled fixture scope");}
  File file=new File(context.getFilesDir(),"reader-identity-time-baseline.json");
  if(!restore&&!file.exists())ReportFiles.write(context,file.getName(),new JSONObject().put("metadata",rows));
  JSONArray originals=new JSONObject(read(file)).getJSONArray("metadata");long earliest=Long.MAX_VALUE;for(int i=0;i<originals.length();i++)earliest=Math.min(earliest,originals.getJSONObject(i).getLong("createdAt"));
  if(earliest<1000)throw new IOException("Invalid fixture creation time");String target=restore?null:new JSONObject(read(new File(context.getFilesDir(),"reader-identity-request.json"))).getString("uuid");int changed=0;
  for(int i=0;i<originals.length();i++){
   JSONObject original=originals.getJSONObject(i);if(!restore&&!original.getString("uuid").equals(target))continue;
   boolean present=false;for(int j=0;j<rows.length();j++)if(original.getString("uuid").equals(rows.getJSONObject(j).getString("uuid"))&&original.getString("nocasePath").equals(rows.getJSONObject(j).getString("nocasePath")))present=true;
   if(!present)throw new IOException("Original fixture alias missing during identity test");
   android.content.ContentValues values=new android.content.ContentValues();values.put("createdAt",restore?original.getLong("createdAt"):earliest-1000);
   int count=context.getContentResolver().update(Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata"),values,"uuid=? AND nocasePath=?",new String[]{original.getString("uuid"),original.getString("nocasePath")});
   if(count!=1)throw new IOException("Identity timestamp write must affect one fixture row");changed++;
  }
  if(changed!=(restore?originals.length():1))throw new IOException("Identity test selected unexpected row count");
  return new JSONObject().put("createdAtRestored",restore).put("fixtureOnly",true).put("changedRows",changed);
 }
 JSONObject restoreFixtureFlags() throws Exception {
  capture();JSONObject rows=new JSONObject(read(new File(context.getFilesDir(),"reader-fixture-private.json")));
  JSONObject baseline=new JSONObject(read(new File(context.getFilesDir(),"reader-fixture-sync-baseline.json")));
  JSONArray metadata=rows.getJSONArray("metadata"),allowed=new JSONObject(read(new File(context.getFilesDir(),"reader-sync-scope.json"))).getJSONArray("bookIds");
  int restored=0;boolean originalPresent=false;
  for(int i=0;i<metadata.length();i++){
   JSONObject row=metadata.getJSONObject(i);String uuid=row.getString("uuid");boolean enrolled=false;
   for(int j=0;j<allowed.length();j++)if(uuid.equals(allowed.getString(j)))enrolled=true;
   if(!enrolled)throw new IOException("Unexpected fixture alias; inspect before restoring");
   boolean original=baseline.getString("uuid").equals(uuid);originalPresent|=original;
   android.content.ContentValues values=new android.content.ContentValues();values.put("userDataSyncStatus",original?baseline.getInt("userDataSyncStatus"):0);values.put("fileSyncStatus",original?baseline.getInt("fileSyncStatus"):0);
   int changed=context.getContentResolver().update(Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata"),values,"uuid=? AND nocasePath=?",new String[]{uuid,row.getString("nocasePath")});
   if(changed!=1)throw new IOException("Reader flag restore must affect one scoped row");restored++;
  }
  if(!originalPresent)throw new IOException("Original fixture UUID missing during restoration");
  return new JSONObject().put("fixtureFlagsRestored",true).put("scopedRowsRestored",restored);
 }
 JSONObject capture() throws Exception {
  File file=new File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),BOOK).getCanonicalFile();
  if(!file.isFile() || file.length()>1048576 || !DIGEST.equals(NativeInk.digest(java.nio.file.Files.readAllBytes(file.toPath()))))throw new IOException("Expected exact authored EPUB fixture");
  NativeNotes notes=new NativeNotes(context);
  String authority="content://com.onyx.content.database.ContentProvider/";
  JSONArray metadata=notes.rows(Uri.parse(authority+"Metadata"),"nocasePath=? OR nocasePath=?",new String[]{file.getPath(),"/sdcard/Download/"+BOOK},true);
  if(metadata.length()>8)throw new IOException("Fixture alias count exceeds scoped diagnostic bound");
  JSONArray bookmarks=new JSONArray(),annotations=new JSONArray();
  java.util.LinkedHashSet<String> identities=new java.util.LinkedHashSet<>();
  for(int i=0;i<metadata.length();i++){
   JSONObject row=metadata.getJSONObject(i);String id=row.optString("idString",""),uuid=row.optString("uuid","");
   if(id.isEmpty()||uuid.isEmpty())throw new IOException("Fixture lacks native identity");identities.add(id);identities.add(uuid);
  }
  if(!identities.isEmpty()){
   String selection="idString IN ("+String.join(",",java.util.Collections.nCopies(identities.size(),"?"))+")";
   String[] args=identities.toArray(new String[0]);
   bookmarks=notes.rows(Uri.parse(authority+"Bookmark"),selection,args,true);
   annotations=notes.rows(Uri.parse(authority+"Annotation"),selection,args,true);
  }
  ReportFiles.write(context,"reader-fixture-private.json",new JSONObject().put("bookSha256",DIGEST).put("metadata",metadata).put("bookmarks",bookmarks).put("annotations",annotations));
  return new JSONObject().put("callerUid",android.os.Process.myUid()).put("readOnly",true).put("bookSha256",DIGEST).put("ambiguousNativeIdentity",metadata.length()>1).put("metadataRows",metadata.length()).put("bookmarkRows",bookmarks.length()).put("annotationRows",annotations.length()).put("privateCapture","reader-fixture-private.json").put("readerSyncQualified",false);
 }
}
