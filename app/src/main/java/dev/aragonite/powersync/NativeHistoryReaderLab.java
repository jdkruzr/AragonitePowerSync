package dev.aragonite.powersync;
import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONObject;
/** Pull/export diagnostics for four separately journaled disposable history books. */
final class NativeHistoryReaderLab {
 private final Context context;
 private final String bookId;
 private static final String PDF_SHA="37962785464fb87d6ae1fedf7850aca2ea01dc4245af43ce49bb3e4c99b58086";
 private static final String[] BOOKS={"39d55e7921014fc28fd1693f8610c0aa","4ed0e47687b74f55b1ce0dbd3bf0c05b","425f03983b99494585e208345030af3b","219d0921ee1f44c1a089e08764714ab4"};
 NativeHistoryReaderLab(Context context) throws Exception {
  this.context=context;
  File f=new File(context.getFilesDir(),"reader-history-request.json");
  if(!f.isFile()||f.length()>1048576)throw new IOException("Bounded history request required");
  JSONObject request=new JSONObject(new String(java.nio.file.Files.readAllBytes(f.toPath()),java.nio.charset.StandardCharsets.UTF_8));
  int index=request.getInt("fixtureIndex");if(index<0||index>=BOOKS.length)throw new IOException("Unknown history fixture");bookId=BOOKS[index];
  if(!"powersync_lab".equals(new NativeSystemConfigProbe().read().getString("curClusterId")))throw new IOException("History requires lab endpoint");
 }
 JSONObject enroll() throws Exception {
  String path="/storage/emulated/0/Download/PowerSync-history-"+bookId+".pdf";
  File pdf=new File(path);if(!pdf.isFile()||pdf.length()!=2340||!PDF_SHA.equals(NativeInk.digest(java.nio.file.Files.readAllBytes(pdf.toPath()))))throw new IOException("Exact owned history PDF required");
  Uri uri=Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata");
  try(android.database.Cursor c=context.getContentResolver().query(uri,new String[]{"uuid"},"uuid=? OR nocasePath=?",new String[]{bookId,path},null)){if(c==null||c.getCount()!=0)throw new IOException("New history metadata must be absent");}
  JSONObject request=new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(context.getFilesDir(),"reader-history-request.json").toPath()),java.nio.charset.StandardCharsets.UTF_8));
  JSONObject row=request.getJSONObject("metadata");
  if(!bookId.equals(row.getString("uuid"))||!path.equals(row.getString("nocasePath"))||!path.equals(row.getString("idString"))||row.has("id")||row.getInt("userDataSyncStatus")!=1||row.getInt("status")!=0)throw new IOException("History metadata identity mismatch");
  android.content.ContentValues values=new android.content.ContentValues();java.util.Iterator<String> keys=row.keys();while(keys.hasNext()){String key=keys.next();Object v=row.get(key);if(v==JSONObject.NULL)values.putNull(key);else if(v instanceof Number)values.put(key,((Number)v).longValue());else if(v instanceof String)values.put(key,(String)v);else throw new IOException("Unexpected metadata value");}
  if(context.getContentResolver().insert(uri,values)==null)throw new IOException("History enrollment failed");
  return new JSONObject().put("bookId",bookId).put("newMetadataInserted",true).put("physicalFreshInstall",false).put("privateDatabasesReset",false);
 }
 JSONObject pullFullSource() throws Exception {
  if(!"powersync_lab".equals(new NativeSystemConfigProbe().read().getString("curClusterId")))throw new IOException("Full PDF pull requires lab endpoint");
  capture();JSONObject captured=new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(context.getFilesDir(),"reader-history-pdf-private.json").toPath()),java.nio.charset.StandardCharsets.UTF_8));JSONArray rows=captured.getJSONArray("metadata");String id=bookId;
  if(rows.length()!=1||!id.equals(rows.getJSONObject(0).getString("uuid")))throw new IOException("Exact owned source identity required");
  JSONObject row=rows.getJSONObject(0);ReportFiles.write(context,"reader-history-pdf-full-pull-extra-before.json",new JSONObject().put("extraInfo",row.get("extraInfo")));
  JSONObject extra=new JSONObject(row.getString("extraInfo"));extra.put("pullFullUserDataFromSync",true);android.content.ContentValues v=new android.content.ContentValues();v.put("extraInfo",extra.toString());
  if(context.getContentResolver().update(Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata"),v,"uuid=? AND nocasePath=?",new String[]{id,"/storage/emulated/0/Download/PowerSync-history-"+bookId+".pdf"})!=1)throw new IOException("One owned full-pull flag update required");
  try {return syncNative(false,false);}
  finally {
   JSONArray current=new NativeNotes(context).rows(Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata"),"uuid=? AND nocasePath=?",new String[]{id,"/storage/emulated/0/Download/PowerSync-history-"+bookId+".pdf"},true);
   if(current.length()!=1)throw new IOException("One owned full-pull flag restoration row required");
   JSONObject restored=new JSONObject(current.getJSONObject(0).getString("extraInfo"));
   restored.put("pullFullUserDataFromSync",new JSONObject(row.getString("extraInfo")).optBoolean("pullFullUserDataFromSync",false));
   android.content.ContentValues restore=new android.content.ContentValues();restore.put("extraInfo",restored.toString());
   if(context.getContentResolver().update(Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata"),restore,"uuid=? AND nocasePath=?",new String[]{id,"/storage/emulated/0/Download/PowerSync-history-"+bookId+".pdf"})!=1)throw new IOException("Full-pull flag restoration failed");
  }
 }
 JSONObject queryFull() throws Exception {return queryFull(false);}
 JSONObject queryMetadata() throws Exception {return queryFull(true);}
 private JSONObject queryFull(boolean metadataOnly) throws Exception {
  if(!"powersync_lab".equals(new NativeSystemConfigProbe().read().getString("curClusterId")))throw new IOException("PDF query requires lab endpoint");
  capture();String id=bookId;
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
     ReportFiles.write(context,"reader-history-pdf-full-userdata-private.json",body);
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
 JSONObject startLibrary(boolean scoped) throws Exception {
  NativeSystemConfigProbe io=new NativeSystemConfigProbe();
  if(!"powersync_lab".equals(io.read().getString("curClusterId"))||!"true".equals(io.read("ONYX_SYSTEM_CONFIG_KEY").getJSONObject("configMap").getString("key_enable_sync_reader_data")))throw new IOException("PDF library restart requires enabled lab reader sync");
  capture();String id=bookId;
  String content=new JSONObject().put("startAllDoc",false).put("startDocIdList",new JSONArray().put(id)).toString();
  try(RemoteBinding binding=new RemoteBinding(context,"com.onyx.android.ksync","com.onyx.android.ksync.service.KSyncService")){
   android.os.IBinder binder=binding.await();String descriptor="com.onyx.android.sdk.sync.ksync.service.ISyncService";
   if(!descriptor.equals(binder.getInterfaceDescriptor()))throw new IOException("Unexpected PDF library interface");
   android.os.Parcel p=android.os.Parcel.obtain();try{
    p.writeInterfaceToken(descriptor);p.writeInt(1);p.writeString("com.onyx.android.sdk.sync.ksync.KSyncContext");p.writeString("dev.aragonite.powersync");p.writeString("powersync-lab");p.writeString(id);p.writeString(null);
    p.writeString(content);p.writeParcelable(null,0);p.writeInt(2);p.writeByte((byte)0);p.writeLong(0);p.writeInt(0);p.writeString(null);p.writeString(null);p.writeByte((byte)0);p.writeInt(0);p.writeByte((byte)0);p.writeLong(0);p.writeString(null);p.writeInt(0);p.writeString(null);p.writeStrongBinder(null);
    if(!binder.transact(scoped?17:16,p,null,android.os.IBinder.FLAG_ONEWAY))throw new IOException("PDF library restart rejected");
   }finally{p.recycle();}
  }
  return new JSONObject().put("requested",true).put("completed",false).put("requestedBookId",id).put("nativeArgumentsHonored",scoped);
 }
 JSONObject syncNative(boolean peer) throws Exception {return syncNative(peer,!peer);}
 JSONObject syncNative(boolean peer,boolean pushing) throws Exception {
  if(!"powersync_lab".equals(new NativeSystemConfigProbe().read().getString("curClusterId")))throw new IOException("PDF sync requires lab endpoint");
  String cloudId=bookId;
  final String id=cloudId;
  String path="/storage/emulated/0/Download/PowerSync-history-"+bookId+".pdf";
  File file=new File(path);
  if(!file.isFile()||file.length()>1048576)throw new IOException("Exact owned PDF required for native sync");
  String pdfDigest=NativeInk.digest(java.nio.file.Files.readAllBytes(file.toPath()));
  if(peer||pushing)throw new IOException("History helper is pull-only");
  if(file.length()!=2340||!PDF_SHA.equals(pdfDigest))throw new IOException("Exact history PDF required");
  int type=pushing?10001:10002;
  java.util.concurrent.CompletableFuture<JSONObject> done=new java.util.concurrent.CompletableFuture<>();
  android.os.Binder callback=new android.os.Binder(){
   @Override protected boolean onTransact(int code,android.os.Parcel data,android.os.Parcel reply,int flags) throws android.os.RemoteException {
    String descriptor="com.onyx.android.sdk.sync.ksync.reader.client.service.ReaderSyncIntentCallback";
    if(code==INTERFACE_TRANSACTION){if(reply!=null)reply.writeString(descriptor);return true;}
    if(code!=1)return super.onTransact(code,data,reply,flags);
    try{
     data.enforceInterface(descriptor);if(data.readInt()!=1)throw new IOException("Empty PDF sync callback");
     String receivedPath=data.readString(),receivedId=data.readString();boolean pull=data.readByte()!=0,push=data.readByte()!=0;
     android.os.ParcelFileDescriptor payload=data.readParcelable(android.os.ParcelFileDescriptor.class.getClassLoader());int receivedType=data.readInt(),result=data.readInt();String message=data.readString();boolean newData=data.readByte()!=0;String toId=data.readString();
     if(payload!=null)payload.close();if(!path.equals(receivedPath)||!id.equals(receivedId)||type!=receivedType)throw new IOException("PDF callback scope mismatch");
     JSONObject event=new JSONObject().put("path",receivedPath).put("fromId",receivedId).put("toId",toId).put("result",result).put("message",message).put("newData",newData).put("pullRequested",pull).put("pushRequested",push);
     ReportFiles.write(context,"reader-history-pdf-sync-callback-private.json",event);done.complete(event);
    }catch(Exception e){done.completeExceptionally(e);}if(reply!=null)reply.writeNoException();return true;
   }
  };
  try(RemoteBinding binding=new RemoteBinding(context,"com.onyx.kreader","com.onyx.android.sdk.readerview.service.ReaderSyncService")){
   android.os.IBinder binder=binding.await();String descriptor="com.onyx.android.sdk.sync.ksync.reader.client.service.IReaderSyncService";
   if(!descriptor.equals(binder.getInterfaceDescriptor()))throw new IOException("Unexpected native PDF sync interface");
   android.os.Parcel p=android.os.Parcel.obtain();try{
    p.writeInterfaceToken(descriptor);p.writeInt(1);p.writeString(path);p.writeString(id);p.writeByte((byte)(pushing?0:1));p.writeByte((byte)(pushing?1:0));p.writeParcelable(null,0);p.writeInt(type);p.writeInt(0);p.writeString(null);p.writeByte((byte)0);p.writeString(null);p.writeStrongBinder(callback);
    if(!binder.transact(3,p,null,android.os.IBinder.FLAG_ONEWAY))throw new IOException("Native PDF sync transaction rejected");
   }finally{p.recycle();}
   return new JSONObject().put("callerUid",android.os.Process.myUid()).put("callback",done.get(40,java.util.concurrent.TimeUnit.SECONDS)).put("nativeInkQualified",false);
  }
 }
 JSONObject export() throws Exception {return export(false);}
 JSONObject export(boolean managed) throws Exception {
  capture(); // Revalidate the exact authored source before contacting native code.
  File source=new File("/storage/emulated/0/Download/PowerSync-history-"+bookId+".pdf");
  if(managed){
   source=new File("/storage/emulated/0/.ksync/powersync-lab/reader/ea6ad9be04304a399583fe557a21f63e/book/ea6ad9be04304a399583fe557a21f63e.pdf");
   if(!source.isFile()||source.length()!=2340||!"37962785464fb87d6ae1fedf7850aca2ea01dc4245af43ce49bb3e4c99b58086".equals(NativeInk.digest(java.nio.file.Files.readAllBytes(source.toPath()))))throw new IOException("Exact managed annotated PDF required");
  }
  File directory=new File("/storage/emulated/0/Download/PowerSync-pdf-export-"+java.util.UUID.randomUUID());
  if(!directory.mkdir())throw new IOException("Fresh export directory required");
  JSONObject request=new JSONObject().put("exportDir",directory.getPath()).put("exportFileName","PowerSync-pdf-ink")
    .put("pathList",new JSONArray().put(source.getPath())).put("convertToPDF",false);
  ReportFiles.write(context,"reader-history-pdf-export-request.json",request);
  java.util.concurrent.CompletableFuture<JSONObject> done=new java.util.concurrent.CompletableFuture<>();
  JSONArray callbacks=new JSONArray();
  android.os.Binder callback=new android.os.Binder(){
   @Override protected boolean onTransact(int code,android.os.Parcel data,android.os.Parcel reply,int flags) throws android.os.RemoteException {
    if(code==INTERFACE_TRANSACTION){reply.writeString("com.onyx.android.sdk.reader.ReaderExportCallback");return true;}
    if(code!=1)return super.onTransact(code,data,reply,flags);
    data.enforceInterface("com.onyx.android.sdk.reader.ReaderExportCallback");
    int kind=data.readInt(),state=data.readInt();String path=data.readString();
    java.util.ArrayList<String> complete=data.createStringArrayList(),errors=data.createStringArrayList();
    try {
     JSONObject event=new JSONObject().put("kind",kind).put("state",state).put("outputPath",path)
       .put("complete",new JSONArray(complete)).put("errors",new JSONArray(errors));
     synchronized(callbacks){callbacks.put(event);ReportFiles.write(context,"reader-history-pdf-export-callbacks.json",new JSONObject().put("callbacks",callbacks));}
     if(kind==0&&(state==1||state==2))done.complete(event);
    }catch(Exception e){done.completeExceptionally(e);}
    reply.writeNoException();return true;
   }
  };
  try(RemoteBinding binding=new RemoteBinding(context,"com.onyx.kreader","com.onyx.android.sdk.readerview.service.ReaderExportService")){
   android.os.IBinder binder=binding.await();String descriptor="com.onyx.android.sdk.reader.IReaderExportService";
   if(!descriptor.equals(binder.getInterfaceDescriptor()))throw new IOException("Unexpected reader export interface");
   android.os.Parcel data=android.os.Parcel.obtain(),reply=android.os.Parcel.obtain();
   try {data.writeInterfaceToken(descriptor);data.writeString(request.toString());data.writeStrongBinder(callback);
    if(!binder.transact(4,data,reply,0))throw new IOException("Reader export transaction rejected");reply.readException();
   }finally{data.recycle();reply.recycle();}
   JSONObject event=done.get(60,java.util.concurrent.TimeUnit.SECONDS);
   File output=new File(event.getString("outputPath")).getCanonicalFile();
   if(event.getInt("state")!=1||event.getJSONArray("errors").length()!=0||event.getJSONArray("complete").length()!=1
      ||!source.getPath().equals(event.getJSONArray("complete").getString(0))
      ||!output.getPath().startsWith(directory.getCanonicalPath()+File.separator)||!output.isFile()||output.length()>67108864)
     throw new IOException("Reader export did not produce one bounded fixture archive");
   return new JSONObject().put("callerUid",android.os.Process.myUid()).put("nativeCallback",event).put("exportExists",true)
     .put("exportBytes",output.length()).put("exportSha256",NativeInk.digest(java.nio.file.Files.readAllBytes(output.toPath())))
     .put("nativeInkQualified",false);
  }
 }
 JSONObject capture() throws Exception {
  String name="PowerSync-history-"+bookId+".pdf";
  String digest="37962785464fb87d6ae1fedf7850aca2ea01dc4245af43ce49bb3e4c99b58086";
  File file=new File("/storage/emulated/0/Download/"+name).getCanonicalFile();
  if(!file.isFile()||file.length()>1048576)throw new IOException("Exact authored geometry PDF required");
  String actual=NativeInk.digest(java.nio.file.Files.readAllBytes(file.toPath()));
  if(file.length()!=2340||!PDF_SHA.equals(actual))throw new IOException("Unqualified history PDF revision");
  digest=actual;
  NativeNotes notes=new NativeNotes(context);
  String base="content://com.onyx.content.database.ContentProvider/";
  JSONArray metadata=notes.rows(Uri.parse(base+"Metadata"),"nocasePath=? OR nocasePath=?",new String[]{file.getPath(),"/sdcard/Download/"+name},true);
  if(metadata.length()!=1||!bookId.equals(metadata.getJSONObject(0).getString("uuid")))throw new IOException("Exact history identity required");
  java.util.LinkedHashSet<String> ids=new java.util.LinkedHashSet<>();
  for(int i=0;i<metadata.length();i++){
   JSONObject row=metadata.getJSONObject(i);
   for(String key:new String[]{"uuid","idString"}){String id=row.getString(key);if(id.isEmpty())throw new IOException("Missing PDF identity");ids.add(id);}
  }
  JSONObject data=new JSONObject().put("bookSha256",digest).put("metadata",metadata);
  if(!ids.isEmpty()){
   String in=" IN ("+String.join(",",java.util.Collections.nCopies(ids.size(),"?"))+")";
   String[] args=ids.toArray(new String[0]);
   for(String table:new String[]{"Bookmark","Annotation"})data.put(table,notes.rows(Uri.parse(base+table),"idString"+in,args,true));
   String ink="content://com.onyx.kreader.note.provider/";
   // Installed manifests name an implementation whose query may be disabled,
   // despite a generated sibling provider containing a usable query method.
   try {data.put("readerNoteDocuments",notes.rows(Uri.parse(ink+"ReaderNoteDocumentModel"),"uniqueId"+in,args,true));}
   catch(UnsupportedOperationException e){data.put("readerNoteDocumentsQueryError",e.toString());}
   try {data.put("readerNoteShapes",notes.rows(Uri.parse(ink+"ReaderNoteShapeModel"),"documentUniqueId"+in,args,true));}
   catch(UnsupportedOperationException e){data.put("readerNoteShapesQueryError",e.toString());}
  }
  ReportFiles.write(context,"reader-history-pdf-private.json",data);
  return new JSONObject().put("callerUid",android.os.Process.myUid()).put("readOnly",true).put("metadataRows",metadata.length()).put("bookSha256",digest).put("privateCapture","reader-history-pdf-private.json").put("nativeGeometryQualified",false).put("nativeInkQualified",false);
 }
}
