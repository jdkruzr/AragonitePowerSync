package dev.aragonite.powersync;

import android.content.Context;
import android.database.Cursor;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Notebook operations restricted to a separately journaled disposable lab fixture. */
final class NativeGatewayLab {
 private final Context context;
 private final FixtureRunner fixture;
 NativeGatewayLab(Context context){this.context=context;fixture=new FixtureRunner(context,"gatewayFixture");}
 private void guard() throws Exception {
  if(!NativeClusterLab.LAB_ID.equals(new NativeClusterLab(context).snapshot().getString("clusterId")))
   throw new IOException("Gateway experiment requires fresh lab cluster observation");
 }
 JSONObject preservation() throws Exception {
  String fixtureId=context.getSharedPreferences("gatewayFixture",0).getString("fixtureId",null);
  Set<String> allowed=new HashSet<>();if(fixtureId!=null)allowed.add(fixtureId);
  File allowedFile=new File(context.getFilesDir(),"gateway-allowed-fixtures.json");
  if(allowedFile.isFile()){
   if(allowedFile.length()>65536)throw new IOException("Fixture allowlist exceeds bound");
   JSONArray ids=new JSONArray(new String(java.nio.file.Files.readAllBytes(allowedFile.toPath()),StandardCharsets.UTF_8));
   for(int i=0;i<ids.length();i++){
    String id=ids.getString(i);if(!id.matches("[0-9a-f-]{32,36}"))throw new IOException("Invalid allowed fixture ID");allowed.add(id);
   }
  }
  List<String> rows=new ArrayList<>();int labRows=0;
  try(Cursor c=context.getContentResolver().query(NativeNotes.NOTES,new String[]{"uniqueId","type","status","userId"},null,null,null)){
   if(c==null)throw new IOException("Missing native library cursor");
   while(c.moveToNext()){
    if("powersync-lab".equals(c.getString(3)))labRows++;
    if(!allowed.contains(c.getString(0)))rows.add(c.getString(0)+":"+c.getInt(1)+":"+c.getInt(2)+":"+c.getString(3));
   }
  }
  Collections.sort(rows);
  JSONObject report=new JSONObject().put("otherRows",rows.size()).put("labAccountRows",labRows)
   .put("otherIdentityStatusOwnershipSha256",NativeInk.digest(String.join("\n",rows).getBytes(StandardCharsets.UTF_8)));
  File baseline=new File(context.getFilesDir(),"gateway-preservation-baseline.json");
  if(!baseline.exists())ReportFiles.write(context,baseline.getName(),report);
  JSONObject before=new JSONObject(new String(java.nio.file.Files.readAllBytes(baseline.toPath()),StandardCharsets.UTF_8));
  return report.put("otherRowsPreserved",before.getString("otherIdentityStatusOwnershipSha256")
   .equals(report.getString("otherIdentityStatusOwnershipSha256")));
 }
 JSONObject create() throws Exception {
  guard();preservation();JSONObject result=fixture.create();
  if(!"powersync-lab".equals(fixture.find().optString("userId")))throw new IOException("Disposable fixture is not owned by lab account");
  return result.put("preservation",preservation());
 }
 JSONObject enable() throws Exception {
  guard();JSONObject row=fixture.find();
  if(!"powersync-lab".equals(row.optString("userId")))throw new IOException("Fixture is not owned by lab account");
  ReportFiles.write(context,"gateway-fixture-transition.json",new JSONObject().put("phase","enable_requested").put("documentId",row.getString("uniqueId")));
  new NativeNotes(context).send("UPDATE_SYNC_STATUS",new JSONObject().put("documentId",row.getString("uniqueId")).put("noteSyncStatus",1),false);
  for(int i=0;i<40;i++){row=fixture.find();if(row.optInt("noteSyncStatus",-1)==1)break;Thread.sleep(250);}
  if(row.optInt("noteSyncStatus",-1)!=1)throw new IOException("Fixture sync enable not observed");
  return new JSONObject().put("note",row).put("preservation",preservation());
 }
 JSONObject capture() throws Exception{return new JSONObject().put("note",fixture.find()).put("preservation",preservation());}
 JSONObject rename() throws Exception{guard();return fixture.rename().put("preservation",preservation());}
 JSONObject disable() throws Exception{return fixture.disableNativeCloud().put("preservation",preservation());}
 JSONObject sharedCapture() throws Exception {
  File file=new File(context.getFilesDir(),"gateway-allowed-fixtures.json");
  if(!file.isFile() || file.length()>65536)throw new IOException("Stage bounded fixture allowlist first");
  JSONArray ids=new JSONArray(new String(java.nio.file.Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));
  JSONArray rows=new JSONArray();
  for(int i=0;i<ids.length();i++){
   String id=ids.getString(i);if(!id.matches("[0-9a-f-]{32,36}"))throw new IOException("Invalid fixture ID");
   JSONArray found=new NativeNotes(context).notes("uniqueId=? AND userId=?",new String[]{id,"powersync-lab"});
   for(int j=0;j<found.length();j++)rows.put(found.getJSONObject(j));
  }
  return new JSONObject().put("notes",rows).put("preservation",preservation());
 }
 JSONObject renameShared() throws Exception {
  guard();JSONObject captured=sharedCapture();JSONArray rows=selectedRows();
  if(rows.length()!=1)throw new IOException("Expected one shared disposable fixture");
  JSONObject row=rows.getJSONObject(0);String id=row.getString("uniqueId");
  String title="PowerSync fixture shared edit from "+android.os.Build.MODEL;
  JSONObject bean=new JSONObject().put("documentId",id).put("title",title).put("parentUniqueId",row.optString("parentUniqueId",""));
  new NativeNotes(context).send("RENAME_NOTE",new JSONObject().put("noteBean",bean),false);
  for(int i=0;i<40;i++){
   JSONObject now=sharedCapture();
   if(title.equals(now.getJSONArray("notes").getJSONObject(0).optString("title")))return now.put("sameIdentity",true);
   Thread.sleep(250);
  }
  throw new IOException("Shared fixture rename not observed");
 }
 JSONObject syncShared() throws Exception {
  return syncShared(false);
 }
 JSONObject syncShared(boolean pull) throws Exception {
  guard();JSONArray rows=selectedRows();
  if(rows.length()!=1)throw new IOException("Expected one disposable fixture");
  String id=rows.getJSONObject(0).getString("uniqueId");
  try(RemoteBinding binding=new RemoteBinding(context,"com.onyx.android.ksync","com.onyx.android.ksync.service.KSyncService")){
   android.os.IBinder binder=binding.await();
   String descriptor="com.onyx.android.sdk.sync.ksync.service.ISyncService";
   if(!descriptor.equals(binder.getInterfaceDescriptor()))throw new IOException("Unexpected sync Binder");
   android.os.Parcel p=android.os.Parcel.obtain();
   try{
    p.writeInterfaceToken(descriptor);p.writeInt(1);
    p.writeString("com.onyx.android.sdk.sync.ksync.KSyncContext");p.writeString("dev.aragonite.powersync");p.writeString("powersync-lab");p.writeString(id);p.writeString(null);
    p.writeString(new JSONArray().put(id).toString());p.writeParcelable(null,0);
    p.writeInt(2);p.writeByte((byte)0);p.writeLong(0);p.writeInt(0);p.writeString(null);p.writeString(null);
    p.writeByte((byte)0);p.writeInt(0);p.writeByte((byte)0);p.writeLong(0);p.writeString(null);p.writeInt(0);p.writeString(null);
    if(pull)p.writeStrongBinder(null);
    if(!binder.transact(pull ? 23 : 25,p,null,android.os.IBinder.FLAG_ONEWAY))throw new IOException("Unsupported scoped sync transaction");
   }finally{p.recycle();}
  }
  return new JSONObject().put("requested",true).put("documentId",id).put("completed",false);
 }
 JSONObject exportShared() throws Exception {
  JSONArray rows=selectedRows();
  if(rows.length()!=1)throw new IOException("Expected one disposable fixture");
  return fixture.exportRow(rows.getJSONObject(0)).put("preservation",preservation());
 }
 private JSONArray selectedRows() throws Exception {
  JSONArray rows=sharedCapture().getJSONArray("notes");
  File selected=new File(context.getFilesDir(),"gateway-selected-fixture.json");
  if(!selected.isFile())return rows;
  if(selected.length()>1024)throw new IOException("Selection exceeds bound");
  String id=new JSONObject(new String(java.nio.file.Files.readAllBytes(selected.toPath()),StandardCharsets.UTF_8)).getString("documentId");
  JSONArray result=new JSONArray();
  for(int i=0;i<rows.length();i++)if(id.equals(rows.getJSONObject(i).getString("uniqueId")))result.put(rows.getJSONObject(i));
  return result;
 }
 android.content.Intent selectedOpen() throws Exception {
  guard();JSONArray rows=selectedRows();if(rows.length()!=1)throw new IOException("Expected one disposable fixture");
  return new NativeNotes(context).openIntent(rows.getJSONObject(0));
 }
 JSONObject selectedCapture() throws Exception {
  JSONArray rows=selectedRows();if(rows.length()!=1)throw new IOException("Expected one selected lab fixture");
  return new JSONObject().put("note",rows.getJSONObject(0)).put("preservation",preservation());
 }
 JSONObject selectedEnable() throws Exception {
  guard();JSONObject row=selectedCapture().getJSONObject("note");String id=row.getString("uniqueId");
  for(int status:new int[]{0,1}){
   new NativeNotes(context).send("UPDATE_SYNC_STATUS",new JSONObject().put("documentId",id).put("noteSyncStatus",status),false);
   boolean observed=false;
   for(int i=0;i<40;i++){if(selectedCapture().getJSONObject("note").optInt("noteSyncStatus",-1)==status){observed=true;break;}Thread.sleep(250);}
   if(!observed)throw new IOException("Selected fixture sync status not observed");
  }
  return selectedCapture();
 }
 JSONObject restartTree() throws Exception {
  guard();
  try(RemoteBinding binding=new RemoteBinding(context,"com.onyx.android.ksync","com.onyx.android.ksync.service.KSyncService")){
   android.os.IBinder binder=binding.await();
   String descriptor="com.onyx.android.sdk.sync.ksync.service.ISyncService";
   if(!descriptor.equals(binder.getInterfaceDescriptor()))throw new IOException("Unexpected sync Binder");
   android.os.Parcel p=android.os.Parcel.obtain();
   try{p.writeInterfaceToken(descriptor);p.writeInt(0);if(!binder.transact(29,p,null,android.os.IBinder.FLAG_ONEWAY))throw new IOException("Unsupported tree restart");}
   finally{p.recycle();}
  }
  return new JSONObject().put("requested",true).put("completed",false);
 }
 // Native pending-tree pull; exports records and may advance bookkeeping. Not a full CBL snapshot.
 JSONObject querySelectedTree() throws Exception {
  guard();String id=selectedCapture().getJSONObject("note").getString("uniqueId");
  final String requested=id;java.util.concurrent.CompletableFuture<JSONObject> result=new java.util.concurrent.CompletableFuture<>();
  android.os.Binder callback=new android.os.Binder(){
   @Override protected boolean onTransact(int code,android.os.Parcel data,android.os.Parcel reply,int flags) throws android.os.RemoteException {
    String descriptor="com.onyx.android.sdk.sync.ksync.service.SyncIntentCallback";
    if(code==android.os.IBinder.INTERFACE_TRANSACTION){if(reply!=null)reply.writeString(descriptor);return true;}
    if(code!=1)return super.onTransact(code,data,reply,flags);
    try{
     data.enforceInterface(descriptor);if(data.readInt()==0)throw new IOException("Empty tree callback");
     if(!"com.onyx.android.sdk.sync.ksync.KSyncContext".equals(data.readString()))throw new IOException("Unexpected tree context");
     data.readString();String user=data.readString(),document=data.readString();data.readString();
     if(!"powersync-lab".equals(user)||!requested.equals(document))throw new IOException("Tree callback identity mismatch");
     String inlineContent=data.readString();android.os.ParcelFileDescriptor payload=data.readParcelable(android.os.ParcelFileDescriptor.class.getClassLoader());
     int status=data.readInt();boolean completed=data.readByte()!=0;data.readLong();int offset=data.readInt();data.readString();data.readString();data.readByte();data.readInt();data.readByte();long total=data.readLong();data.readString();data.readInt();String protoPath=data.readString();
     if(status!=2)throw new IOException("Tree query failed: "+status);
     JSONObject body;
     if(payload==null){Object value=new JSONTokener(inlineContent==null?"{}":inlineContent).nextValue();body=value instanceof JSONObject?(JSONObject)value:new JSONObject().put("tinyRecordList",value);}else try(java.io.InputStream stream=new android.os.ParcelFileDescriptor.AutoCloseInputStream(payload)){
      java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;
      while((count=stream.read(buffer))!=-1){if(bytes.size()+count>4194304)throw new IOException("Tree query exceeds bound");bytes.write(buffer,0,count);}
      body=new JSONObject(new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8));
     }
     ReportFiles.write(context,"gateway-tree-query-private.json",new JSONObject().put("inlineContent",body).put("protoFilePath",protoPath).put("offset",offset).put("totalCount",total).put("completed",completed));
     result.complete(new JSONObject().put("nativeTreePullRequested",true).put("fullDatabaseSnapshot",false).put("callbackStatus",status).put("completed",completed).put("offset",offset).put("totalCount",total).put("tinyRecords",body.optJSONArray("tinyRecordList")==null?0:body.getJSONArray("tinyRecordList").length()));
    }catch(Exception e){result.completeExceptionally(e);}
    if(reply!=null)reply.writeNoException();return true;
   }
  };
  try(RemoteBinding binding=new RemoteBinding(context,"com.onyx.android.ksync","com.onyx.android.ksync.service.KSyncService")){
   android.os.IBinder binder=binding.await();String descriptor="com.onyx.android.sdk.sync.ksync.service.ISyncService";
   if(!descriptor.equals(binder.getInterfaceDescriptor()))throw new IOException("Unexpected tree Binder");
   android.os.Parcel parcel=android.os.Parcel.obtain();try{writeTreeQueryArgs(parcel,id,callback);if(!binder.transact(21,parcel,null,android.os.IBinder.FLAG_ONEWAY))throw new IOException("Tree query rejected");}finally{parcel.recycle();}
   return result.get(35,java.util.concurrent.TimeUnit.SECONDS);
  }
 }
 private void writeTreeQueryArgs(android.os.Parcel p,String id,android.os.IBinder callback){
  p.writeInterfaceToken("com.onyx.android.sdk.sync.ksync.service.ISyncService");p.writeInt(1);p.writeString("com.onyx.android.sdk.sync.ksync.KSyncContext");p.writeString("dev.aragonite.powersync");p.writeString("powersync-lab");p.writeString(id);p.writeString(null);
  p.writeString(null);p.writeParcelable(null,0);p.writeInt(2);p.writeByte((byte)0);p.writeLong(0);p.writeInt(0);p.writeString(null);p.writeString(null);p.writeByte((byte)0);p.writeInt(0);p.writeByte((byte)0);p.writeLong(0);p.writeString(null);p.writeInt(0);p.writeString(null);p.writeStrongBinder(callback);
 }
}
