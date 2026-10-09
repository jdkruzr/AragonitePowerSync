package dev.aragonite.powersync;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONObject;

/** Bounded diagnostics and native operations for one exact, locally authored PDF. */
final class NativeReaderPdfLab {
 private final Context context;
 NativeReaderPdfLab(Context context) { this.context=context; }
 JSONObject stagePeerMetadata() throws Exception {
  String cloud="cbd2938779124b2b83413f76dbe8ab12",path="/storage/emulated/0/.ksync/powersync-lab/reader/"+cloud+"/book/"+cloud+".pdf";
  if(!"powersync_lab".equals(new NativeSystemConfigProbe().read().getString("curClusterId"))||!"c0e26f46d2f365c13c2cf64bc8efc4b5eb756ba8b899060fdf9222d4d8a566a7".equals(NativeInk.digest(java.nio.file.Files.readAllBytes(new File(path).toPath()))))throw new IOException("Owned lab PDF required");
  File request=new File(context.getFilesDir(),"reader-pdf-peer-metadata-request.json");if(!request.isFile()||request.length()>1048576)throw new IOException("Bounded fixture metadata required");
  JSONObject row=new JSONObject(new String(java.nio.file.Files.readAllBytes(request.toPath()),java.nio.charset.StandardCharsets.UTF_8));
  if(!cloud.equals(row.getString("uuid"))||!path.equals(row.getString("nocasePath")))throw new IOException("Unexpected peer fixture identity");
  Uri uri=Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata");
  try(android.database.Cursor c=context.getContentResolver().query(uri,new String[]{"uuid","nocasePath"},"uuid=? OR nocasePath=?",new String[]{cloud,path},null)){
   if(c==null)throw new IOException("Missing peer metadata cursor");
   if(c.getCount()==1&&c.moveToFirst()&&cloud.equals(c.getString(0))&&"/storage/emulated/0/Download/PowerSync-reader-geometry.pdf".equals(c.getString(1))){
    android.content.ContentValues v=new android.content.ContentValues();v.put("nocasePath",path);v.put("fileSyncStatus",0);v.put("userDataSyncStatus",1);
    if(context.getContentResolver().update(uri,v,"uuid=? AND nocasePath=?",new String[]{cloud,c.getString(1)})!=1)throw new IOException("Cloud fixture alias relocation failed");
    return new JSONObject().put("fixtureMetadataStaged",true).put("cloudFixtureAliasRelocated",true).put("ordinaryUid",android.os.Process.myUid());
   }
   if(c.getCount()!=0)throw new IOException("Peer fixture must be absent before staging");
  }
  android.content.ContentValues values=new android.content.ContentValues();java.util.Iterator<String> keys=row.keys();while(keys.hasNext()){String key=keys.next();Object v=row.get(key);if(v==JSONObject.NULL)values.putNull(key);else if(v instanceof Number)values.put(key,((Number)v).longValue());else if(v instanceof String)values.put(key,(String)v);else throw new IOException("Unsupported fixture column");}
  if(context.getContentResolver().insert(uri,values)==null)throw new IOException("Peer fixture insert failed");
  return new JSONObject().put("fixtureMetadataStaged",true).put("ordinaryUid",android.os.Process.myUid());
 }
 JSONObject enrollPeer() throws Exception {
  String cloud="cbd2938779124b2b83413f76dbe8ab12",local="dd9c0167293c411b9df712f89ff98af6";
  String path="/storage/emulated/0/.ksync/powersync-lab/reader/"+cloud+"/book/"+cloud+".pdf";
  if(!"powersync_lab".equals(new NativeSystemConfigProbe().read().getString("curClusterId"))||!"c0e26f46d2f365c13c2cf64bc8efc4b5eb756ba8b899060fdf9222d4d8a566a7".equals(NativeInk.digest(java.nio.file.Files.readAllBytes(new File(path).toPath()))))throw new IOException("Owned lab peer PDF required");
  android.content.ContentValues v=new android.content.ContentValues();v.put("uuid",cloud);
  int changed=context.getContentResolver().update(Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata"),v,"uuid=? AND nocasePath=?",new String[]{local,path});
  if(changed!=1)throw new IOException("One owned managed peer identity required");
  return new JSONObject().put("fixtureIdentityEnrolled",true).put("changedRows",changed);
 }
 JSONObject restoreOwned() throws Exception {
  String cloud="cbd2938779124b2b83413f76dbe8ab12";File baseline=new File(context.getFilesDir(),"reader-pdf-sync-baseline.json");int restored=0,removed=0;
  if(baseline.isFile()){
   JSONObject b=new JSONObject(new String(java.nio.file.Files.readAllBytes(baseline.toPath()),java.nio.charset.StandardCharsets.UTF_8));
   if(!cloud.equals(b.getString("uuid"))||!"/storage/emulated/0/Download/PowerSync-reader-geometry.pdf".equals(b.getString("nocasePath")))throw new IOException("Unexpected source fixture baseline");
   android.content.ContentValues v=new android.content.ContentValues();v.put("userDataSyncStatus",b.getInt("userDataSyncStatus"));v.put("fileSyncStatus",b.getInt("fileSyncStatus"));
   restored=context.getContentResolver().update(Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata"),v,"uuid=? AND nocasePath=?",new String[]{cloud,b.getString("nocasePath")});if(restored!=1)throw new IOException("Source flag restoration failed");
  }else{
   String path="/storage/emulated/0/.ksync/powersync-lab/reader/"+cloud+"/book/"+cloud+".pdf";
   removed=context.getContentResolver().delete(Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata"),"(uuid=? OR uuid=?) AND nocasePath=?",new String[]{cloud,"dd9c0167293c411b9df712f89ff98af6",path});if(removed!=1)throw new IOException("One owned managed alias cleanup required");
  }
  return new JSONObject().put("sourceFlagsRestored",restored).put("ownedManagedAliasesRemoved",removed);
 }
 JSONObject pullFullSource() throws Exception {
  if(!"powersync_lab".equals(new NativeSystemConfigProbe().read().getString("curClusterId")))throw new IOException("Full PDF pull requires lab endpoint");
  capture();JSONObject captured=new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(context.getFilesDir(),"reader-pdf-private.json").toPath()),java.nio.charset.StandardCharsets.UTF_8));JSONArray rows=captured.getJSONArray("metadata");String id="cbd2938779124b2b83413f76dbe8ab12";
  if(rows.length()!=1||!id.equals(rows.getJSONObject(0).getString("uuid")))throw new IOException("Exact owned source identity required");
  JSONObject row=rows.getJSONObject(0);ReportFiles.write(context,"reader-pdf-full-pull-extra-before.json",new JSONObject().put("extraInfo",row.get("extraInfo")));
  JSONObject extra=new JSONObject(row.getString("extraInfo"));extra.put("pullFullUserDataFromSync",true);android.content.ContentValues v=new android.content.ContentValues();v.put("extraInfo",extra.toString());
  if(context.getContentResolver().update(Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata"),v,"uuid=? AND nocasePath=?",new String[]{id,"/storage/emulated/0/Download/PowerSync-reader-geometry.pdf"})!=1)throw new IOException("One owned full-pull flag update required");
  return syncNative(false,false);
 }
 JSONObject publishMetadata() throws Exception {
  if(!"powersync_lab".equals(new NativeSystemConfigProbe().read().getString("curClusterId")))throw new IOException("PDF metadata publication requires lab endpoint");
  capture();JSONObject data=new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(context.getFilesDir(),"reader-pdf-private.json").toPath()),java.nio.charset.StandardCharsets.UTF_8));
  String id="cbd2938779124b2b83413f76dbe8ab12";JSONArray rows=data.getJSONArray("metadata");
  if(rows.length()!=1||!id.equals(rows.getJSONObject(0).getString("uuid"))||!"c0e26f46d2f365c13c2cf64bc8efc4b5eb756ba8b899060fdf9222d4d8a566a7".equals(data.getString("bookSha256")))throw new IOException("Exact source PDF metadata required");
  JSONObject row=rows.getJSONObject(0);
  File baseline=new File(context.getFilesDir(),"reader-pdf-sync-baseline.json");if(!baseline.exists())ReportFiles.write(context,baseline.getName(),row);
  android.content.ContentValues enabled=new android.content.ContentValues();enabled.put("userDataSyncStatus",1);
  if(context.getContentResolver().update(Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata"),enabled,"uuid=? AND nocasePath=?",new String[]{id,"/storage/emulated/0/Download/PowerSync-reader-geometry.pdf"})!=1)throw new IOException("One owned PDF eligibility update required");
  row.put("uniqueId",id).put("user","powersync-lab").put("dbId","powersync-lab-READER_LIBRARY").put("modeType",4).put("userDataSyncStatus",1);
  JSONObject body=new JSONObject().put("metadataModelList",new JSONArray().put(row)).put("libraryModelList",new JSONArray()).put("collectionModelList",new JSONArray()).put("requestType","SYNC_DOCUMENT").put("isSyncUserOtherData",false);
  File request=new File(context.getFilesDir(),"reader-pdf-metadata-request.json");ReportFiles.write(context,request.getName(),body);
  try(android.os.ParcelFileDescriptor fd=android.os.ParcelFileDescriptor.open(request,android.os.ParcelFileDescriptor.MODE_READ_ONLY);
      RemoteBinding binding=new RemoteBinding(context,"com.onyx.android.ksync","com.onyx.android.ksync.service.KSyncService")){
   android.os.IBinder binder=binding.await();if(!"com.onyx.android.sdk.sync.ksync.service.ISyncService".equals(binder.getInterfaceDescriptor()))throw new IOException("Unexpected PDF metadata interface");
   android.os.Parcel p=android.os.Parcel.obtain();try{writeArgs(p,id,null,fd,null);if(!binder.transact(1,p,null,android.os.IBinder.FLAG_ONEWAY))throw new IOException("PDF metadata publication rejected");}finally{p.recycle();}
  }
  return new JSONObject().put("requested",true).put("completed",false).put("fixedBookScope",id);
 }
 JSONObject queryFull() throws Exception {return queryFull(false);}
 JSONObject queryMetadata() throws Exception {return queryFull(true);}
 private JSONObject queryFull(boolean metadataOnly) throws Exception {
  if(!"powersync_lab".equals(new NativeSystemConfigProbe().read().getString("curClusterId")))throw new IOException("PDF query requires lab endpoint");
  capture();String id="cbd2938779124b2b83413f76dbe8ab12";
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
     ReportFiles.write(context,"reader-pdf-full-userdata-private.json",body);
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
 JSONObject restartLibrary() throws Exception {return startLibrary(false);}
 JSONObject startLibrary(boolean scoped) throws Exception {
  NativeSystemConfigProbe io=new NativeSystemConfigProbe();
  if(!"powersync_lab".equals(io.read().getString("curClusterId"))||!"true".equals(io.read("ONYX_SYSTEM_CONFIG_KEY").getJSONObject("configMap").getString("key_enable_sync_reader_data")))throw new IOException("PDF library restart requires enabled lab reader sync");
  capture();String id="cbd2938779124b2b83413f76dbe8ab12";
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
  String cloudId="cbd2938779124b2b83413f76dbe8ab12";
  final String id=cloudId;
  String path=peer?"/storage/emulated/0/.ksync/powersync-lab/reader/"+cloudId+"/book/"+cloudId+".pdf":"/storage/emulated/0/Download/PowerSync-reader-geometry.pdf";
  File file=new File(path);
  if(!file.isFile()||file.length()>1048576)throw new IOException("Exact owned PDF required for native sync");
  String pdfDigest=NativeInk.digest(java.nio.file.Files.readAllBytes(file.toPath()));
  boolean annotated=file.length()==22562&&"c0e26f46d2f365c13c2cf64bc8efc4b5eb756ba8b899060fdf9222d4d8a566a7".equals(pdfDigest);
  boolean original=!peer&&file.length()==2270&&"6ccfeddb1f27d9d62975d78f0ec7d4c3f4c1bb8af69aa9786b5cd3974fa443bd".equals(pdfDigest);
  if(!annotated&&!original)throw new IOException("Exact owned PDF revision required for native sync");
  if(!peer&&pushing){
   capture();JSONObject captured=new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(context.getFilesDir(),"reader-pdf-private.json").toPath()),java.nio.charset.StandardCharsets.UTF_8));
   JSONArray rows=captured.getJSONArray("metadata");if(rows.length()!=1||!id.equals(rows.getJSONObject(0).getString("uuid")))throw new IOException("Owned source PDF identity required");
   File baseline=new File(context.getFilesDir(),"reader-pdf-sync-baseline.json");if(!baseline.exists())ReportFiles.write(context,baseline.getName(),rows.getJSONObject(0));
   android.content.ContentValues values=new android.content.ContentValues();values.put("userDataSyncStatus",1);
   if(context.getContentResolver().update(Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata"),values,"uuid=? AND nocasePath=?",new String[]{id,path})!=1)throw new IOException("One source PDF eligibility update required");
  }
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
     ReportFiles.write(context,"reader-pdf-sync-callback-private.json",event);done.complete(event);
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
 JSONObject transfer(boolean upload) throws Exception {
  if(!"powersync_lab".equals(new NativeSystemConfigProbe().read().getString("curClusterId")))throw new IOException("PDF transfer requires lab endpoint");
  capture();
  File request=new File(context.getFilesDir(),"reader-pdf-transfer-request.json");
  if(!request.isFile()||request.length()>1048576)throw new IOException("Bounded PDF request required");
  JSONObject body=new JSONObject(new String(java.nio.file.Files.readAllBytes(request.toPath()),java.nio.charset.StandardCharsets.UTF_8));
  JSONArray models=body.getJSONArray("metadataModelList");if(models.length()!=1)throw new IOException("One PDF model required");
  JSONObject model=models.getJSONObject(0);String id=model.getString("uniqueId");
  if(!"cbd2938779124b2b83413f76dbe8ab12".equals(id)||!id.equals(model.getString("uuid"))
     ||!"powersync-lab".equals(model.getString("user"))||!"powersync-lab-READER_LIBRARY".equals(model.getString("dbId"))
     ||model.getInt("modeType")!=4||!"PowerSync-reader-geometry.pdf".equals(model.getString("name"))
     ||!"/storage/emulated/0/Download/PowerSync-reader-geometry.pdf".equals(model.getString("nativeAbsolutePath"))
     ||model.getLong("fileOriginSize")!=22562)throw new IOException("PDF transfer outside exact owned fixture");
  if(upload){
   JSONObject current=new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(context.getFilesDir(),"reader-pdf-private.json").toPath()),java.nio.charset.StandardCharsets.UTF_8));
   if(!"c0e26f46d2f365c13c2cf64bc8efc4b5eb756ba8b899060fdf9222d4d8a566a7".equals(current.getString("bookSha256"))
      ||current.getJSONArray("metadata").length()!=1||!id.equals(current.getJSONArray("metadata").getJSONObject(0).getString("uuid")))throw new IOException("Native annotated source required");
  }
  try(android.os.ParcelFileDescriptor payload=android.os.ParcelFileDescriptor.open(request,android.os.ParcelFileDescriptor.MODE_READ_ONLY);
      RemoteBinding binding=new RemoteBinding(context,"com.onyx.android.ksync","com.onyx.android.ksync.service.KSyncService")){
   android.os.IBinder binder=binding.await();String descriptor="com.onyx.android.sdk.sync.ksync.service.ISyncService";
   if(!descriptor.equals(binder.getInterfaceDescriptor()))throw new IOException("Unexpected PDF transfer interface");
   android.os.Parcel p=android.os.Parcel.obtain();try{
    p.writeInterfaceToken(descriptor);p.writeInt(1);p.writeString("com.onyx.android.sdk.sync.ksync.KSyncContext");p.writeString("dev.aragonite.powersync");p.writeString("powersync-lab");p.writeString(id);p.writeString(null);
    p.writeString(null);p.writeParcelable(payload,0);p.writeInt(2);p.writeByte((byte)0);p.writeLong(0);p.writeInt(0);p.writeString(null);p.writeString(null);p.writeByte((byte)0);p.writeInt(0);p.writeByte((byte)0);p.writeLong(0);p.writeString(null);p.writeInt(0);p.writeString(null);p.writeStrongBinder(null);
    if(!binder.transact(upload?12:13,p,null,android.os.IBinder.FLAG_ONEWAY))throw new IOException("PDF transfer rejected");
   }finally{p.recycle();}
  }
  return new JSONObject().put("callerUid",android.os.Process.myUid()).put("requested",true).put("completed",false).put("operation",upload?"owned-pdf-upload":"owned-pdf-download");
 }
 JSONObject export() throws Exception {return export(false);}
 JSONObject export(boolean managed) throws Exception {
  capture(); // Revalidate the exact authored source before contacting native code.
  File source=new File("/storage/emulated/0/Download/PowerSync-reader-geometry.pdf");
  if(managed){
   source=new File("/storage/emulated/0/.ksync/powersync-lab/reader/cbd2938779124b2b83413f76dbe8ab12/book/cbd2938779124b2b83413f76dbe8ab12.pdf");
   if(!source.isFile()||source.length()!=22562||!"c0e26f46d2f365c13c2cf64bc8efc4b5eb756ba8b899060fdf9222d4d8a566a7".equals(NativeInk.digest(java.nio.file.Files.readAllBytes(source.toPath()))))throw new IOException("Exact managed annotated PDF required");
  }
  File directory=new File("/storage/emulated/0/Download/PowerSync-pdf-export-"+java.util.UUID.randomUUID());
  if(!directory.mkdir())throw new IOException("Fresh export directory required");
  JSONObject request=new JSONObject().put("exportDir",directory.getPath()).put("exportFileName","PowerSync-pdf-ink")
    .put("pathList",new JSONArray().put(source.getPath())).put("convertToPDF",false);
  ReportFiles.write(context,"reader-pdf-export-request.json",request);
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
     synchronized(callbacks){callbacks.put(event);ReportFiles.write(context,"reader-pdf-export-callbacks.json",new JSONObject().put("callbacks",callbacks));}
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
  String name="PowerSync-reader-geometry.pdf";
  String digest="6ccfeddb1f27d9d62975d78f0ec7d4c3f4c1bb8af69aa9786b5cd3974fa443bd";
  File file=new File("/storage/emulated/0/Download/"+name).getCanonicalFile();
  if(!file.isFile()||file.length()>1048576)throw new IOException("Exact authored geometry PDF required");
  String actual=NativeInk.digest(java.nio.file.Files.readAllBytes(file.toPath()));
  // Exact user-authored native derivative observed after the owned pen control.
  if(!digest.equals(actual)&&!"c0e26f46d2f365c13c2cf64bc8efc4b5eb756ba8b899060fdf9222d4d8a566a7".equals(actual))throw new IOException("Unqualified PDF revision");
  digest=actual;
  NativeNotes notes=new NativeNotes(context);
  String base="content://com.onyx.content.database.ContentProvider/";
  JSONArray metadata=notes.rows(Uri.parse(base+"Metadata"),"nocasePath=? OR nocasePath=?",new String[]{file.getPath(),"/sdcard/Download/"+name},true);
  if(metadata.length()>8)throw new IOException("PDF identity bound exceeded");
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
  ReportFiles.write(context,"reader-pdf-private.json",data);
  return new JSONObject().put("callerUid",android.os.Process.myUid()).put("readOnly",true).put("metadataRows",metadata.length()).put("bookSha256",digest).put("privateCapture","reader-pdf-private.json").put("nativeGeometryQualified",false).put("nativeInkQualified",false);
 }
}
