package dev.aragonite.powersync;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONObject;

/** Bounded diagnostics and native operations for one exact, locally authored PDF. */
final class NativeFreshReaderPdfLab {
 private final Context context;
 NativeFreshReaderPdfLab(Context context) { this.context=context; }
 JSONObject notifyConfigChange() throws Exception {
  NativeSystemConfigProbe probe=new NativeSystemConfigProbe();
  if(!"powersync_lab".equals(probe.read().getString("curClusterId")))throw new IOException("Config notification requires lab endpoint");
  JSONObject config=probe.read("ONYX_SYSTEM_CONFIG_KEY");
  if(!"true".equals(config.getJSONObject("configMap").getString("key_enable_sync_reader_data"))||!"false".equals(config.getJSONObject("configMap").getString("key_enable_sync_note_data")))throw new IOException("Reader-only lab flags required");
  for(String target:new String[]{"com.onyx.kreader","com.onyx.android.ksync"})context.sendBroadcast(new android.content.Intent("com.onyx.action.system.config.changed").setPackage(target));
  return new JSONObject().put("callerUid",android.os.Process.myUid()).put("requested",true).put("deliveryObserved",false).put("systemFlagsWritten",false).put("action","com.onyx.action.system.config.changed");
 }
 JSONObject enableAutoSync() throws Exception {
  if(!"powersync_lab".equals(new NativeSystemConfigProbe().read().getString("curClusterId")))throw new IOException("Fresh reader auto sync requires lab endpoint");
  capture();
  JSONObject data=new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(context.getFilesDir(),"reader-fresh-pdf-private.json").toPath()),java.nio.charset.StandardCharsets.UTF_8));
  JSONArray rows=data.getJSONArray("metadata");
  if(rows.length()!=1)throw new IOException("One fresh fixture row required");
  JSONObject row=rows.getJSONObject(0);
  String id="ea6ad9be04304a399583fe557a21f63e",path="/storage/emulated/0/Download/PowerSync-reader-clean-v2.pdf";
  if(!id.equals(row.getString("uuid"))||!path.equals(row.getString("nocasePath")))throw new IOException("Unexpected fresh fixture identity");
  JSONObject extra=new JSONObject(row.getString("extraInfo"));
  ReportFiles.write(context,"reader-fresh-auto-sync-baseline.json",new JSONObject().put("id",row.getLong("id")).put("uuid",id).put("path",path).put("extraInfo",extra.toString()));
  extra.put("autoSync",true);
  android.content.ContentValues values=new android.content.ContentValues();values.put("extraInfo",extra.toString());
  int changed=context.getContentResolver().update(Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata"),values,"id=? AND uuid=? AND nocasePath=?",new String[]{row.getString("id"),id,path});
  if(changed!=1)throw new IOException("One fresh fixture auto-sync update required");
  return new JSONObject().put("ordinaryUid",android.os.Process.myUid()).put("changedRows",changed).put("autoSync",true);
 }
 JSONObject stagePeerMetadata() throws Exception {
  String cloud="ea6ad9be04304a399583fe557a21f63e",path="/storage/emulated/0/.ksync/powersync-lab/reader/"+cloud+"/book/"+cloud+".pdf";
  if(!"powersync_lab".equals(new NativeSystemConfigProbe().read().getString("curClusterId"))||!"37962785464fb87d6ae1fedf7850aca2ea01dc4245af43ce49bb3e4c99b58086".equals(NativeInk.digest(java.nio.file.Files.readAllBytes(new File(path).toPath()))))throw new IOException("Owned lab PDF required");
  File request=new File(context.getFilesDir(),"reader-fresh-pdf-peer-metadata-request.json");if(!request.isFile()||request.length()>1048576)throw new IOException("Bounded fixture metadata required");
  JSONObject row=new JSONObject(new String(java.nio.file.Files.readAllBytes(request.toPath()),java.nio.charset.StandardCharsets.UTF_8));
  if(!cloud.equals(row.getString("uuid"))||!path.equals(row.getString("nocasePath")))throw new IOException("Unexpected peer fixture identity");
  Uri uri=Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata");
  try(android.database.Cursor c=context.getContentResolver().query(uri,new String[]{"uuid","nocasePath"},"uuid=? OR nocasePath=?",new String[]{cloud,path},null)){
   if(c==null)throw new IOException("Missing peer metadata cursor");
   if(c.getCount()==1&&c.moveToFirst()&&cloud.equals(c.getString(0))&&"/storage/emulated/0/Download/PowerSync-reader-clean-v2.pdf".equals(c.getString(1))){
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
  String cloud="ea6ad9be04304a399583fe557a21f63e",local="dd9c0167293c411b9df712f89ff98af6";
  String path="/storage/emulated/0/.ksync/powersync-lab/reader/"+cloud+"/book/"+cloud+".pdf";
  if(!"powersync_lab".equals(new NativeSystemConfigProbe().read().getString("curClusterId"))||!"37962785464fb87d6ae1fedf7850aca2ea01dc4245af43ce49bb3e4c99b58086".equals(NativeInk.digest(java.nio.file.Files.readAllBytes(new File(path).toPath()))))throw new IOException("Owned lab peer PDF required");
  android.content.ContentValues v=new android.content.ContentValues();v.put("uuid",cloud);
  int changed=context.getContentResolver().update(Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata"),v,"uuid=? AND nocasePath=?",new String[]{local,path});
  if(changed!=1)throw new IOException("One owned managed peer identity required");
  return new JSONObject().put("fixtureIdentityEnrolled",true).put("changedRows",changed);
 }
 JSONObject restoreOwned() throws Exception {
  String cloud="ea6ad9be04304a399583fe557a21f63e";File baseline=new File(context.getFilesDir(),"reader-fresh-pdf-sync-baseline.json");int restored=0,removed=0;
  if(baseline.isFile()){
   JSONObject b=new JSONObject(new String(java.nio.file.Files.readAllBytes(baseline.toPath()),java.nio.charset.StandardCharsets.UTF_8));
   if(!cloud.equals(b.getString("uuid"))||!"/storage/emulated/0/Download/PowerSync-reader-clean-v2.pdf".equals(b.getString("nocasePath")))throw new IOException("Unexpected source fixture baseline");
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
  capture();JSONObject captured=new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(context.getFilesDir(),"reader-fresh-pdf-private.json").toPath()),java.nio.charset.StandardCharsets.UTF_8));JSONArray rows=captured.getJSONArray("metadata");String id="ea6ad9be04304a399583fe557a21f63e";
  if(rows.length()!=1||!id.equals(rows.getJSONObject(0).getString("uuid")))throw new IOException("Exact owned source identity required");
  JSONObject row=rows.getJSONObject(0);ReportFiles.write(context,"reader-fresh-pdf-full-pull-extra-before.json",new JSONObject().put("extraInfo",row.get("extraInfo")));
  JSONObject extra=new JSONObject(row.getString("extraInfo"));extra.put("pullFullUserDataFromSync",true);android.content.ContentValues v=new android.content.ContentValues();v.put("extraInfo",extra.toString());
  if(context.getContentResolver().update(Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata"),v,"uuid=? AND nocasePath=?",new String[]{id,"/storage/emulated/0/Download/PowerSync-reader-clean-v2.pdf"})!=1)throw new IOException("One owned full-pull flag update required");
  try {return syncNative(false,false);}
  finally {
   JSONArray current=new NativeNotes(context).rows(Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata"),"uuid=? AND nocasePath=?",new String[]{id,"/storage/emulated/0/Download/PowerSync-reader-clean-v2.pdf"},true);
   if(current.length()!=1)throw new IOException("One owned full-pull flag restoration row required");
   JSONObject restored=new JSONObject(current.getJSONObject(0).getString("extraInfo"));
   restored.put("pullFullUserDataFromSync",new JSONObject(row.getString("extraInfo")).optBoolean("pullFullUserDataFromSync",false));
   android.content.ContentValues restore=new android.content.ContentValues();restore.put("extraInfo",restored.toString());
   if(context.getContentResolver().update(Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata"),restore,"uuid=? AND nocasePath=?",new String[]{id,"/storage/emulated/0/Download/PowerSync-reader-clean-v2.pdf"})!=1)throw new IOException("Full-pull flag restoration failed");
  }
 }
 JSONObject publishMetadata() throws Exception {
  if(!"powersync_lab".equals(new NativeSystemConfigProbe().read().getString("curClusterId")))throw new IOException("PDF metadata publication requires lab endpoint");
  capture();JSONObject data=new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(context.getFilesDir(),"reader-fresh-pdf-private.json").toPath()),java.nio.charset.StandardCharsets.UTF_8));
  String id="ea6ad9be04304a399583fe557a21f63e";JSONArray rows=data.getJSONArray("metadata");
  if(rows.length()!=1||!id.equals(rows.getJSONObject(0).getString("uuid"))||!"37962785464fb87d6ae1fedf7850aca2ea01dc4245af43ce49bb3e4c99b58086".equals(data.getString("bookSha256")))throw new IOException("Exact source PDF metadata required");
  JSONObject row=rows.getJSONObject(0);
  File baseline=new File(context.getFilesDir(),"reader-fresh-pdf-sync-baseline.json");if(!baseline.exists())ReportFiles.write(context,baseline.getName(),row);
  android.content.ContentValues enabled=new android.content.ContentValues();enabled.put("userDataSyncStatus",1);
  if(context.getContentResolver().update(Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata"),enabled,"uuid=? AND nocasePath=?",new String[]{id,"/storage/emulated/0/Download/PowerSync-reader-clean-v2.pdf"})!=1)throw new IOException("One owned PDF eligibility update required");
  row.put("uniqueId",id).put("user","powersync-lab").put("dbId","powersync-lab-READER_LIBRARY").put("modeType",4).put("userDataSyncStatus",1);
  JSONObject body=new JSONObject().put("metadataModelList",new JSONArray().put(row)).put("libraryModelList",new JSONArray()).put("collectionModelList",new JSONArray()).put("requestType","SYNC_DOCUMENT").put("isSyncUserOtherData",false);
  File request=new File(context.getFilesDir(),"reader-fresh-pdf-metadata-request.json");ReportFiles.write(context,request.getName(),body);
  try(android.os.ParcelFileDescriptor fd=android.os.ParcelFileDescriptor.open(request,android.os.ParcelFileDescriptor.MODE_READ_ONLY);
      RemoteBinding binding=new RemoteBinding(context,"com.onyx.android.ksync","com.onyx.android.ksync.service.KSyncService")){
   android.os.IBinder binder=binding.await();if(!"com.onyx.android.sdk.sync.ksync.service.ISyncService".equals(binder.getInterfaceDescriptor()))throw new IOException("Unexpected PDF metadata interface");
   android.os.Parcel p=android.os.Parcel.obtain();try{writeArgs(p,id,null,fd,null);if(!binder.transact(1,p,null,android.os.IBinder.FLAG_ONEWAY))throw new IOException("PDF metadata publication rejected");}finally{p.recycle();}
  }
  return new JSONObject().put("requested",true).put("completed",false).put("fixedBookScope",id);
 }
 JSONObject restartOwnedDocJob() throws Exception {
  NativeSystemConfigProbe io=new NativeSystemConfigProbe();JSONObject flags=io.read("ONYX_SYSTEM_CONFIG_KEY").getJSONObject("configMap");
  if(!"powersync_lab".equals(io.read().getString("curClusterId"))||!"true".equals(flags.getString("key_enable_sync_reader_data"))||!"false".equals(flags.getString("key_enable_sync_note_data"))||!"d13306a5e1166eeb9325271dda84cad325fa10ac82b7efc4f367f70a1645d523".equals(capture().getString("bookSha256")))throw new IOException("Exact pending owned Z lab source required");
  String book="ea6ad9be04304a399583fe557a21f63e";
  try(RemoteBinding binding=new RemoteBinding(context,"com.onyx.android.ksync","com.onyx.android.ksync.service.KSyncService")){
   android.os.IBinder binder=binding.await();String descriptor="com.onyx.android.sdk.sync.ksync.service.ISyncService";if(!descriptor.equals(binder.getInterfaceDescriptor()))throw new IOException("Unexpected owned-job Binder");
   android.os.Parcel parcel=android.os.Parcel.obtain();try{parcel.writeInterfaceToken(descriptor);parcel.writeString(book);if(!binder.transact(14,parcel,null,android.os.IBinder.FLAG_ONEWAY))throw new IOException("Owned document stop rejected");}finally{parcel.recycle();}
  }
  Thread.sleep(1500);
  return startLibrary(true).put("ownedDocStopRequested",true).put("completed",false).put("callerUid",android.os.Process.myUid()).put("bodyFieldsChanged",false);
 }
 JSONObject requeueCurrentUserData() throws Exception {
  NativeSystemConfigProbe io=new NativeSystemConfigProbe();
  JSONObject flags=io.read("ONYX_SYSTEM_CONFIG_KEY").getJSONObject("configMap");
  if(!"powersync_lab".equals(io.read().getString("curClusterId"))||!"true".equals(flags.getString("key_enable_sync_reader_data"))||!"false".equals(flags.getString("key_enable_sync_note_data")))throw new IOException("Owned reader-only lab requeue required");
  JSONObject captured=capture();
  if(!"d13306a5e1166eeb9325271dda84cad325fa10ac82b7efc4f367f70a1645d523".equals(captured.getString("bookSha256")))throw new IOException("Exact saved Z source required");
  queryFull();
  JSONObject current=new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(context.getFilesDir(),"reader-fresh-pdf-full-userdata-private.json").toPath()),java.nio.charset.StandardCharsets.UTF_8));
  JSONArray models=current.getJSONArray("noteDocumentModelList");
  String book="ea6ad9be04304a399583fe557a21f63e";
  if(models.length()!=1)throw new IOException("Exactly one current owned model required");
  JSONObject model=models.getJSONObject(0);
  if(model.getInt("modeType")!=7||!book.equals(model.getString("documentId"))||!"e61b286c49594b1686b4bca987a8274b".equals(model.getString("uniqueId"))||!"powersync-lab".equals(model.getString("user"))||!"powersync-lab-READER_LIBRARY".equals(model.getString("dbId")))throw new IOException("Owned model identity required");
  JSONObject payload=new JSONObject().put("noteDocumentModelList",models);
  for(String list:new String[]{"annotationModelList","bookmarkModelList"}){
   JSONArray values=current.getJSONArray(list);
   for(int i=0;i<values.length();i++)if(!book.equals(values.getJSONObject(i).optString("documentId"))||!"powersync-lab".equals(values.getJSONObject(i).optString("user")))throw new IOException("Owned user-data rows required");
   payload.put(list,values);
  }
  File body=new File(context.getFilesDir(),"reader-fresh-requeue-payload-private.json");ReportFiles.write(context,body.getName(),payload);
  String hash=NativeInk.digest(java.nio.file.Files.readAllBytes(body.toPath()));
  try(RemoteBinding binding=new RemoteBinding(context,"com.onyx.android.ksync","com.onyx.android.ksync.service.KSyncService");android.os.ParcelFileDescriptor fd=android.os.ParcelFileDescriptor.open(body,android.os.ParcelFileDescriptor.MODE_READ_ONLY)){
   android.os.IBinder binder=binding.await();if(!"com.onyx.android.sdk.sync.ksync.service.ISyncService".equals(binder.getInterfaceDescriptor()))throw new IOException("Unexpected requeue Binder");
   android.os.Parcel parcel=android.os.Parcel.obtain();try{writeArgs(parcel,book,null,fd,null);if(!binder.transact(3,parcel,null,android.os.IBinder.FLAG_ONEWAY))throw new IOException("Owned native requeue rejected");}finally{parcel.recycle();}
  }
  return new JSONObject().put("requested",true).put("completed",false).put("callerUid",android.os.Process.myUid()).put("fixedBookScope",book).put("payloadSha256",hash).put("bodyFieldsChanged",false).put("nativeApplicationObserved",false);
 }
 JSONObject publishConfigConflict() throws Exception {
  queryFull();
  File request=new File(context.getFilesDir(),"reader-fresh-config-request.json");
  if(!request.isFile()||request.length()>1048576)throw new IOException("Bounded fresh config request required");
  JSONObject proposed=new JSONObject(new String(java.nio.file.Files.readAllBytes(request.toPath()),java.nio.charset.StandardCharsets.UTF_8));
  JSONObject currentBody=new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(context.getFilesDir(),"reader-fresh-pdf-full-userdata-private.json").toPath()),java.nio.charset.StandardCharsets.UTF_8));
  JSONArray models=currentBody.getJSONArray("noteDocumentModelList");
  if(models.length()!=1)throw new IOException("Exactly one native configuration required");
  JSONObject baseline=models.getJSONObject(0), expected=new JSONObject(baseline.toString());
  String book="ea6ad9be04304a399583fe557a21f63e",id="e61b286c49594b1686b4bca987a8274b";
  if(baseline.getInt("modeType")!=7||!book.equals(baseline.getString("documentId"))||!id.equals(baseline.getString("uniqueId"))||!"powersync-lab".equals(baseline.getString("user"))||!"powersync-lab-READER_LIBRARY".equals(baseline.getString("dbId")))throw new IOException("Unexpected native configuration identity");
  double width=proposed.getDouble("strokeWidth");long stamp=proposed.getLong("updatedAt");
  if((width!=3.0&&width!=4.0)||Math.abs(System.currentTimeMillis()-stamp)>300000)throw new IOException("Fixture config changes out of bounds");
  expected.put("strokeWidth",width).put("updatedAt",stamp);
  expected.remove("_id");expected.remove("_rev");proposed.remove("_id");proposed.remove("_rev");
  if(expected.length()!=proposed.length())throw new IOException("Configuration field count changed");
  java.util.Iterator<String> keys=expected.keys();while(keys.hasNext()){String key=keys.next();if(!proposed.has(key)||!String.valueOf(expected.get(key)).equals(String.valueOf(proposed.get(key))))throw new IOException("Unexpected configuration field change: "+key);}
  ReportFiles.write(context,"reader-fresh-config-baseline-private.json",baseline);
  JSONObject payload=new JSONObject().put("annotationModelList",new JSONArray()).put("bookmarkModelList",new JSONArray()).put("noteDocumentModelList",new JSONArray().put(expected));
  File body=new File(context.getFilesDir(),"reader-fresh-config-publication-private.json");ReportFiles.write(context,body.getName(),payload);
  try(android.os.ParcelFileDescriptor fd=android.os.ParcelFileDescriptor.open(body,android.os.ParcelFileDescriptor.MODE_READ_ONLY);RemoteBinding binding=new RemoteBinding(context,"com.onyx.android.ksync","com.onyx.android.ksync.service.KSyncService")){
   android.os.IBinder binder=binding.await();if(!"com.onyx.android.sdk.sync.ksync.service.ISyncService".equals(binder.getInterfaceDescriptor()))throw new IOException("Unexpected configuration Binder");
   android.os.Parcel parcel=android.os.Parcel.obtain();try{writeArgs(parcel,book,null,fd,null);if(!binder.transact(3,parcel,null,android.os.IBinder.FLAG_ONEWAY))throw new IOException("Configuration save rejected");}finally{parcel.recycle();}
  }
  return new JSONObject().put("requested",true).put("completed",false).put("nativeCblSaveRequested",true).put("strokeWidth",width).put("updatedAt",stamp);
 }
 JSONObject queryFull() throws Exception {return queryFull(false);}
 JSONObject queryMetadata() throws Exception {return queryFull(true);}
 private JSONObject queryFull(boolean metadataOnly) throws Exception {
  if(!"powersync_lab".equals(new NativeSystemConfigProbe().read().getString("curClusterId")))throw new IOException("PDF query requires lab endpoint");
  capture();String id="ea6ad9be04304a399583fe557a21f63e";
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
     ReportFiles.write(context,"reader-fresh-pdf-full-userdata-private.json",body);
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
  capture();String id="ea6ad9be04304a399583fe557a21f63e";
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
  String cloudId="ea6ad9be04304a399583fe557a21f63e";
  final String id=cloudId;
  String path=peer?"/storage/emulated/0/.ksync/powersync-lab/reader/"+cloudId+"/book/"+cloudId+".pdf":"/storage/emulated/0/Download/PowerSync-reader-clean-v2.pdf";
  File file=new File(path);
  if(!file.isFile()||file.length()>1048576)throw new IOException("Exact owned PDF required for native sync");
  String pdfDigest=NativeInk.digest(java.nio.file.Files.readAllBytes(file.toPath()));
  boolean annotated=file.length()==14954&&"7930cf31cc219cd1171a5e030b4e4059663ad628bd24d55d89f150a24efb072d".equals(pdfDigest);
  boolean original=!peer&&file.length()==2340&&"37962785464fb87d6ae1fedf7850aca2ea01dc4245af43ce49bb3e4c99b58086".equals(pdfDigest);
  boolean erased=!peer&&!pushing&&file.length()==27316&&"b08e9ad5e105beb38e404fd180bfb5615dd5fd8ebeda39815ed8d11da4ebdb00".equals(pdfDigest);
  boolean concurrent=!peer&&!pushing&&((file.length()==36768&&"d9d7560d3bd96f74123de2f7d5507d78a30f9f0030d14dd489a54067293ccd10".equals(pdfDigest))||(file.length()==8739&&"0db1571f6e2a37293f28197aa8bf2dc60086e05ffbdf54d90f8421a75673f548".equals(pdfDigest)));
  boolean eraseConflict=!peer&&!pushing&&((file.length()==61789&&"379cb58a840c151d8da42b237f4b125146df3503b8bedab3b499dcb2869714b7".equals(pdfDigest))||(file.length()==9243&&"e0010ed52c8c4d14cb8a3b9f81234eea796c2d5416b6dced55b714e775d0c5e3".equals(pdfDigest)));
  boolean knownFragmentDelete=!peer&&!pushing&&file.length()==9747&&"5fdc19a98b8cd8a89b8b4d3ac552ce25cc5659903d4145c4258705f38c7607ba".equals(pdfDigest);
  boolean uploadZ=!peer&&!pushing&&file.length()==160865&&"d13306a5e1166eeb9325271dda84cad325fa10ac82b7efc4f367f70a1645d523".equals(pdfDigest);
  boolean uploadY=!peer&&!pushing&&file.length()==143130&&"093b37531c50885146872d94956f8ff0c590e84661f9bda113eff0486e622bf1".equals(pdfDigest);
  boolean outageX=!peer&&!pushing&&((file.length()==127750&&"34b74956cc8f561052c794dbd744747874a61bf949b4e846e48750e6ab5d71a9".equals(pdfDigest))||(file.length()==136411&&"75df8c3c6a1f6cc68315762a17c5dcb89ea7b2ae8ec32ea7c1fd6583df698874".equals(pdfDigest)));
  boolean outageW=!peer&&!pushing&&((file.length()==114957&&"c2b02313b16efb3a47e32a56ee2aa4047af196c31eda5d955e00372d4fa24ece".equals(pdfDigest))||(file.length()==123626&&"05fac939312d7597414b6f7b91dcebb063b2af59dc84a4523c68a5d7a87a240d".equals(pdfDigest)));
  boolean unattendedV=!peer&&!pushing&&((file.length()==97531&&"1642b7925b2406fc5e6098d8f5f2df8de09eb1852ce18845225b6b5660a32a3d".equals(pdfDigest))||(file.length()==106207&&"d5f2a2836a209a822d962a52a59aa45cf7cbf16c95d8fdeecd316bcc3bd400e4".equals(pdfDigest)));
  boolean unattendedU=!peer&&!pushing&&((file.length()==80799&&"5dafb8b0046c8bef1531fd198db0272a9c32e93200d431c40e61bb1b4e398477".equals(pdfDigest))||(file.length()==89482&&"049ad2f875b26e7ec147c02188cb56e6d3f972b2ec477ccb0a0a558b38532952".equals(pdfDigest)));
  boolean eraseRestoreConflict=!peer&&!pushing&&((file.length()==71572&&"fd59291c795bedbb44dc97850d75559f4fa775bd6043686b6f1d2957eba6bcc7".equals(pdfDigest))||(file.length()==62881&&"2b9d3ada159e5972ec9182df483bcdc5b15b05bd91639aa4b9e67ee1cac4d101".equals(pdfDigest))||(file.length()==62375&&"4040deb8c6dee297bc3db62e6ad891d3a2e2edceccf96a5277974aa9dd5a1006".equals(pdfDigest))||(file.length()==52987&&"34770e3d44abc508b78f46336a87997350381316305f778440a789d44a09b4fd".equals(pdfDigest)));
  if(!uploadZ&&!uploadY&&!outageX&&!outageW&&!unattendedV&&!unattendedU&&!annotated&&!original&&!erased&&!concurrent&&!eraseConflict&&!knownFragmentDelete&&!eraseRestoreConflict)throw new IOException("Exact owned PDF revision required for native sync");
  if(!peer&&pushing){
   capture();JSONObject captured=new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(context.getFilesDir(),"reader-fresh-pdf-private.json").toPath()),java.nio.charset.StandardCharsets.UTF_8));
   JSONArray rows=captured.getJSONArray("metadata");if(rows.length()!=1||!id.equals(rows.getJSONObject(0).getString("uuid")))throw new IOException("Owned source PDF identity required");
   File baseline=new File(context.getFilesDir(),"reader-fresh-pdf-sync-baseline.json");if(!baseline.exists())ReportFiles.write(context,baseline.getName(),rows.getJSONObject(0));
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
     ReportFiles.write(context,"reader-fresh-pdf-sync-callback-private.json",event);done.complete(event);
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
  File request=new File(context.getFilesDir(),"reader-fresh-pdf-transfer-request.json");
  if(!request.isFile()||request.length()>1048576)throw new IOException("Bounded PDF request required");
  JSONObject body=new JSONObject(new String(java.nio.file.Files.readAllBytes(request.toPath()),java.nio.charset.StandardCharsets.UTF_8));
  JSONArray models=body.getJSONArray("metadataModelList");if(models.length()!=1)throw new IOException("One PDF model required");
  JSONObject model=models.getJSONObject(0);String id=model.getString("uniqueId");
  if(!"ea6ad9be04304a399583fe557a21f63e".equals(id)||!id.equals(model.getString("uuid"))
     ||!"powersync-lab".equals(model.getString("user"))||!"powersync-lab-READER_LIBRARY".equals(model.getString("dbId"))
     ||model.getInt("modeType")!=4||!"PowerSync-reader-clean-v2.pdf".equals(model.getString("name"))
     ||!"/storage/emulated/0/Download/PowerSync-reader-clean-v2.pdf".equals(model.getString("nativeAbsolutePath"))
     ||model.getLong("fileOriginSize")!=2340)throw new IOException("PDF transfer outside exact owned fixture");
  if(upload){
   JSONObject current=new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(context.getFilesDir(),"reader-fresh-pdf-private.json").toPath()),java.nio.charset.StandardCharsets.UTF_8));
   if(!"37962785464fb87d6ae1fedf7850aca2ea01dc4245af43ce49bb3e4c99b58086".equals(current.getString("bookSha256"))
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
  File source=new File("/storage/emulated/0/Download/PowerSync-reader-clean-v2.pdf");
  if(managed){
   source=new File("/storage/emulated/0/.ksync/powersync-lab/reader/ea6ad9be04304a399583fe557a21f63e/book/ea6ad9be04304a399583fe557a21f63e.pdf");
   if(!source.isFile()||source.length()!=2340||!"37962785464fb87d6ae1fedf7850aca2ea01dc4245af43ce49bb3e4c99b58086".equals(NativeInk.digest(java.nio.file.Files.readAllBytes(source.toPath()))))throw new IOException("Exact managed annotated PDF required");
  }
  File directory=new File("/storage/emulated/0/Download/PowerSync-pdf-export-"+java.util.UUID.randomUUID());
  if(!directory.mkdir())throw new IOException("Fresh export directory required");
  JSONObject request=new JSONObject().put("exportDir",directory.getPath()).put("exportFileName","PowerSync-pdf-ink")
    .put("pathList",new JSONArray().put(source.getPath())).put("convertToPDF",false);
  ReportFiles.write(context,"reader-fresh-pdf-export-request.json",request);
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
     synchronized(callbacks){callbacks.put(event);ReportFiles.write(context,"reader-fresh-pdf-export-callbacks.json",new JSONObject().put("callbacks",callbacks));}
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
  String name="PowerSync-reader-clean-v2.pdf";
  String digest="37962785464fb87d6ae1fedf7850aca2ea01dc4245af43ce49bb3e4c99b58086";
  File file=new File("/storage/emulated/0/Download/"+name).getCanonicalFile();
  if(!file.isFile()||file.length()>1048576)throw new IOException("Exact authored geometry PDF required");
  String actual=NativeInk.digest(java.nio.file.Files.readAllBytes(file.toPath()));
  // Exact user-authored native derivative observed after the owned pen control.
  if(!"8d6c8d2e9cf21c4d4f70a815278489c7ac4f9f3ca03ba46c55d8867f246b9091".equals(actual)&&!"5a42b55895a28d61073e63fb245cf468ee1a4f42b831755dc91f608566f78e5a".equals(actual)&&!"3945945fbb6d155067a14d257f943c28a1f031e67158cb536f33e325b80164a0".equals(actual)&&!"0e15a4483159deb0667f7fc1b7cd668934b62c34c7758836b972aa2a009e9179".equals(actual)&&!"f9a289ffd2e2f86bd298519290092381dce4a718212857840ef7f174764ac235".equals(actual)&&!"af2f34bc90d8fd1f89b8055698ae4a04cd7d07029cf8c662a99fb0879ea2d24a".equals(actual)&&!"369320f68a3ac8f56de14ec7c387b86ee4b38844b65b958fdb49af79fe9f684c".equals(actual)&&!"3238b63a12fe7efc9b17af7c56f9fb05de78c5f4f739d3e0c8b90050c6d8105a".equals(actual)&&!"d13306a5e1166eeb9325271dda84cad325fa10ac82b7efc4f367f70a1645d523".equals(actual)&&!"093b37531c50885146872d94956f8ff0c590e84661f9bda113eff0486e622bf1".equals(actual)&&!"34b74956cc8f561052c794dbd744747874a61bf949b4e846e48750e6ab5d71a9".equals(actual)&&!"75df8c3c6a1f6cc68315762a17c5dcb89ea7b2ae8ec32ea7c1fd6583df698874".equals(actual)&&!"c2b02313b16efb3a47e32a56ee2aa4047af196c31eda5d955e00372d4fa24ece".equals(actual)&&!"05fac939312d7597414b6f7b91dcebb063b2af59dc84a4523c68a5d7a87a240d".equals(actual)&&!"1642b7925b2406fc5e6098d8f5f2df8de09eb1852ce18845225b6b5660a32a3d".equals(actual)&&!"d5f2a2836a209a822d962a52a59aa45cf7cbf16c95d8fdeecd316bcc3bd400e4".equals(actual)&&!"049ad2f875b26e7ec147c02188cb56e6d3f972b2ec477ccb0a0a558b38532952".equals(actual)&&!"5dafb8b0046c8bef1531fd198db0272a9c32e93200d431c40e61bb1b4e398477".equals(actual)&&!"fd59291c795bedbb44dc97850d75559f4fa775bd6043686b6f1d2957eba6bcc7".equals(actual)&&!"852f4dda74f06642fbc8481fe0de01b30055c10e7991635feebec415e383649c".equals(actual)&&!"2b9d3ada159e5972ec9182df483bcdc5b15b05bd91639aa4b9e67ee1cac4d101".equals(actual)&&!"4040deb8c6dee297bc3db62e6ad891d3a2e2edceccf96a5277974aa9dd5a1006".equals(actual)&&!"34770e3d44abc508b78f46336a87997350381316305f778440a789d44a09b4fd".equals(actual)&&!"bb6111ea548fcc870d5d5ae8e7d739b6d6f153abe88bf5ef78caa251f221294e".equals(actual)&&!"5fdc19a98b8cd8a89b8b4d3ac552ce25cc5659903d4145c4258705f38c7607ba".equals(actual)&&!"379cb58a840c151d8da42b237f4b125146df3503b8bedab3b499dcb2869714b7".equals(actual)&&!"e0010ed52c8c4d14cb8a3b9f81234eea796c2d5416b6dced55b714e775d0c5e3".equals(actual)&&!"d9d7560d3bd96f74123de2f7d5507d78a30f9f0030d14dd489a54067293ccd10".equals(actual)&&!"0db1571f6e2a37293f28197aa8bf2dc60086e05ffbdf54d90f8421a75673f548".equals(actual)&&!digest.equals(actual)&&!"7930cf31cc219cd1171a5e030b4e4059663ad628bd24d55d89f150a24efb072d".equals(actual)&&!"b08e9ad5e105beb38e404fd180bfb5615dd5fd8ebeda39815ed8d11da4ebdb00".equals(actual))throw new IOException("Unqualified PDF revision");
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
  ReportFiles.write(context,"reader-fresh-pdf-private.json",data);
  return new JSONObject().put("callerUid",android.os.Process.myUid()).put("readOnly",true).put("metadataRows",metadata.length()).put("bookSha256",digest).put("privateCapture","reader-fresh-pdf-private.json").put("nativeGeometryQualified",false).put("nativeInkQualified",false);
 }
}
