package dev.aragonite.powersync;

import android.content.Context;
import android.database.Cursor;
import android.os.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Debug-only stock-ksync endpoint experiment. Never accepts arbitrary target IDs. */
final class NativeClusterLab {
 static final String LAB_ID="powersync_lab";
 private static final String DESCRIPTOR="com.onyx.android.sdk.sync.ksync.service.ISyncService";
 private static final String CALLBACK="com.onyx.android.sdk.sync.ksync.service.SyncClusterCallback";
 private final Context context;
 NativeClusterLab(Context context) {this.context=context;}
 private JSONObject cluster() throws Exception {
  return new NativeSystemConfigProbe().read();
 }
 private JSONObject library() throws Exception {
  List<String> identities=new ArrayList<>();int documents=0;
  try(Cursor c=context.getContentResolver().query(NativeNotes.NOTES,new String[]{"uniqueId","type","status"},null,null,null)) {
   if(c==null) throw new IOException("No native metadata cursor");
   while(c.moveToNext()) {identities.add(c.getString(0)+":"+c.getInt(1)+":"+c.getInt(2));if(c.getInt(1)==1)documents++;}
  }
  Collections.sort(identities);
  return new JSONObject().put("rows",identities.size()).put("documents",documents)
   .put("identityStatusSha256",NativeInk.digest(String.join("\n",identities).getBytes(StandardCharsets.UTF_8)));
 }
 JSONObject snapshot() throws Exception {
  JSONObject config=cluster();
  return new JSONObject().put("clusterId",config.getString("curClusterId"))
   .put("configurationSource","live ordinary-app system-service read")
   .put("availableClusterIds",config.getJSONObject("clusterMap").names()).put("library",library());
 }
 JSONObject baseline() throws Exception {
  File baseline=new File(context.getFilesDir(),"cluster-baseline.json");
  if(baseline.exists()) return new JSONObject().put("alreadyRecorded",true).put("current",snapshot());
  JSONObject config=cluster();
  if(LAB_ID.equals(config.getString("curClusterId"))) throw new IOException("Already on lab cluster without baseline");
  JSONObject report=snapshot().put("clusterConfiguration",config);
  ReportFiles.write(context,"cluster-baseline.json",report);return snapshot().put("baselineRecorded",true);
 }
 JSONObject update() throws Exception {return call(81,null).put("current",snapshot());}
 JSONObject change(boolean restore) throws Exception {
  File baseline=new File(context.getFilesDir(),"cluster-baseline.json");
  if(!baseline.isFile()) throw new IOException("Record cluster baseline first");
  JSONObject before=new JSONObject(new String(java.nio.file.Files.readAllBytes(baseline.toPath()),StandardCharsets.UTF_8));
  String target=restore ? before.getString("clusterId") : LAB_ID;
  JSONObject config=cluster();
  if(!config.getJSONObject("clusterMap").has(target)) throw new IOException("Target missing from native cluster list");
  if(!restore && !before.getJSONObject("library").getString("identityStatusSha256").equals(library().getString("identityStatusSha256"))){
   // A previous isolated round trip added an explicitly allowlisted lab notebook.
   // Require the existing ownership/status baseline for every other row.
   if(!new File(context.getFilesDir(),"gateway-preservation-baseline.json").isFile()
      || !new NativeGatewayLab(context).preservation().getBoolean("otherRowsPreserved"))
    throw new IOException("Library identities changed outside preserved lab fixtures; inspect before switch");
  }
  ReportFiles.write(context,"cluster-transition.json",new JSONObject().put("target",target).put("phase","requested"));
  JSONObject result=call(80,target).put("library",library()).put("requestedTarget",target)
   .put("configurationObservationRequiresRefresh",true);
  boolean preserved=before.getJSONObject("library").getString("identityStatusSha256")
   .equals(result.getJSONObject("library").getString("identityStatusSha256"));
  result.put("libraryIdentitiesAndStatusesPreserved",preserved);
  ReportFiles.write(context,"cluster-transition.json",result);return result;
 }
 private JSONObject call(int transaction,String id) throws Exception {
  CompletableFuture<JSONObject> result=new CompletableFuture<>();
  Binder callback=new Binder() {
   protected boolean onTransact(int code,Parcel data,Parcel reply,int flags) throws android.os.RemoteException {
    if(code==IBinder.INTERFACE_TRANSACTION) {if(reply!=null)reply.writeString(CALLBACK);return true;}
    if(code!=1) return super.onTransact(code,data,reply,flags);
    data.enforceInterface(CALLBACK);
    try {
     if(data.readInt()==0) throw new IOException("Empty cluster callback");
     String callbackId=data.readString();int status=data.readInt();
     result.complete(new JSONObject().put("callbackStatus",status).put("success",status==2)
      .put("callbackClusterId",callbackId==null ? JSONObject.NULL : callbackId));
    } catch(Exception e) {result.completeExceptionally(e);}
    if(reply!=null)reply.writeNoException();return true;
   }
  };
  try(RemoteBinding binding=new RemoteBinding(context,"com.onyx.android.ksync","com.onyx.android.ksync.service.KSyncService")) {
   IBinder binder=binding.await();
   if(!DESCRIPTOR.equals(binder.getInterfaceDescriptor())) throw new IOException("Unexpected ksync Binder");
   Parcel data=Parcel.obtain();
   try {data.writeInterfaceToken(DESCRIPTOR);data.writeInt(1);data.writeString(id);data.writeInt(2);data.writeStrongBinder(callback);
    if(!binder.transact(transaction,data,null,IBinder.FLAG_ONEWAY))throw new IOException("Unsupported cluster transaction");
   } finally {data.recycle();}
   return result.get(60,TimeUnit.SECONDS);
  }
 }
}
