package dev.aragonite.powersync;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import org.json.JSONObject;
import java.io.IOException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** Fixed disposable account only. Login ABI recovered from this Palma APK. */
final class NativeAccountLab {
 private static final String DESCRIPTOR="com.onyx.android.sdk.sync.ksync.service.ISyncService";
 private static final String CALLBACK="com.onyx.android.sdk.sync.ksync.service.AccountIntentCallback";
 private final Context context;
 NativeAccountLab(Context context) {this.context=context;}
 private static final Uri ACCOUNTS=Uri.parse("content://com.onyx.account.database.ContentProvider.KSyncAccountContentProvider/OnyxAccountModel");
 JSONObject probeLabAccountInfo() throws Exception {
  if(!NativeClusterLab.LAB_ID.equals(new NativeSystemConfigProbe().read().getString("curClusterId")))throw new IOException("Requires lab cluster");
  try(Cursor c=context.getContentResolver().query(ACCOUNTS,new String[]{"uid","clusterId"},"loggedIn=1",null,null)){
   if(c==null || c.getCount()!=1 || !c.moveToFirst() || !"powersync-lab".equals(c.getString(0)) || !NativeClusterLab.LAB_ID.equals(c.getString(1)))throw new IOException("Requires sole logged-in disposable account");
  }
  try(RemoteBinding binding=new RemoteBinding(context,"com.onyx.android.ksync","com.onyx.android.ksync.service.KSyncService")){
   IBinder binder=binding.await();if(!DESCRIPTOR.equals(binder.getInterfaceDescriptor()))throw new IOException("Unexpected account Binder");
   Parcel p=Parcel.obtain();try{p.writeInterfaceToken(DESCRIPTOR);p.writeInt(0);p.writeStrongBinder(null);
    if(!binder.transact(69,p,null,IBinder.FLAG_ONEWAY))throw new IOException("Unsupported account-info transaction");
   }finally{p.recycle();}
  }
  return new JSONObject().put("requested",true).put("completed",false).put("disposableAccountOnly",true);
 }
 JSONObject baseline() throws Exception {
  if(!"Go103_2Lumi".equals(Build.MODEL))throw new IOException("Cached account restoration qualified on Lumi only");
  File file=new File(context.getFilesDir(),"account-baseline.json");
  if(file.exists())return new JSONObject().put("alreadyRecorded",true);
  JSONObject account=null;
  try(Cursor c=context.getContentResolver().query(ACCOUNTS,new String[]{"uid","parent","clusterId"},"loggedIn=1",null,null)){
   if(c==null || c.getCount()!=1 || !c.moveToFirst())throw new IOException("Expected one logged-in original account");
   if(c.getString(1)!=null && !c.getString(1).isEmpty())throw new IOException("Original account is not a master account");
   account=new JSONObject().put("uid",c.getString(0)).put("clusterId",c.getString(2));
  }
  if(masterCount(account.getString("uid"))!=1)throw new IOException("Original cached master is not unique");
  ReportFiles.write(context,file.getName(),account);
  return new JSONObject().put("recorded",true).put("credentialsReadOrExported",false);
 }
 private int masterCount(String required) throws Exception {
  if("powersync-lab".equals(required))return 0;
  try(Cursor c=context.getContentResolver().query(ACCOUNTS,new String[]{"uid"},"(parent IS NULL OR parent='') AND uid<>?",new String[]{"powersync-lab"},null)){
   if(c==null || c.getCount()!=1 || !c.moveToFirst() || !required.equals(c.getString(0)))return 0;
   return 1;
  }
 }
 JSONObject restoreOriginal() throws Exception {
  if(!"Go103_2Lumi".equals(Build.MODEL))throw new IOException("Cached restoration qualified on Lumi only");
  File file=new File(context.getFilesDir(),"account-baseline.json");
  JSONObject saved=new JSONObject(new String(java.nio.file.Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));
  JSONObject current=new NativeClusterLab(context).snapshot();
  if(!saved.getString("clusterId").equals(current.getString("clusterId")) || masterCount(saved.getString("uid"))!=1)
   throw new IOException("Original cluster and sole cached master required before restore");
  // Native MASTER selects the most recent master, not a requested UID. Remove
  // only the fixed disposable account cache so the preserved original is unique.
  int removed=context.getContentResolver().delete(ACCOUNTS,"uid=?",new String[]{"powersync-lab"});
  try(Cursor c=context.getContentResolver().query(ACCOUNTS,new String[]{"uid"},"parent IS NULL OR parent=''",null,null)){
   if(c==null || c.getCount()!=1 || !c.moveToFirst() || !saved.getString("uid").equals(c.getString(0)))
    throw new IOException("Original master must be unique after disposable cache cleanup");
  }
  JSONObject result=call(current,true).put("disposableAccountCacheRowsRemoved",removed);
  try(Cursor c=context.getContentResolver().query(ACCOUNTS,new String[]{"uid"},"loggedIn=1",null,null)){
   result.put("originalAccountRestored",c!=null && c.getCount()==1 && c.moveToFirst() && saved.getString("uid").equals(c.getString(0)));
  }
  return result;
 }
 JSONObject login() throws Exception {
  JSONObject before=new NativeClusterLab(context).snapshot();
  if(!NativeClusterLab.LAB_ID.equals(before.getString("clusterId")))throw new IOException("Disposable login requires observed lab cluster");
  if(!"Palma2_Pro_C".equals(Build.MODEL) && !"Go103_2Lumi".equals(Build.MODEL))throw new IOException("Account ABI qualified only on the two lab devices");
  if("Go103_2Lumi".equals(Build.MODEL) && !new File(context.getFilesDir(),"account-baseline.json").isFile())throw new IOException("Record original Lumi account first");
  return call(before,false);
 }
 private JSONObject call(JSONObject before,boolean restore) throws Exception {
  ReportFiles.write(context,"account-transition.json",new JSONObject().put("phase","login_requested").put("before",before));
  CompletableFuture<Integer> result=new CompletableFuture<>();
  Binder callback=new Binder() {
   protected boolean onTransact(int code,Parcel data,Parcel reply,int flags) throws RemoteException {
    if(code==IBinder.INTERFACE_TRANSACTION){if(reply!=null)reply.writeString(CALLBACK);return true;}
    if(code!=1)return super.onTransact(code,data,reply,flags);
    data.enforceInterface(CALLBACK);
    if(data.readInt()==0)result.completeExceptionally(new IOException("Empty account callback"));
    else {
     // Read only the bounded, fixed Parcelable fields. Never retain credentials.
     data.readString();data.readString();data.readString();data.readInt();
     data.readString();data.readString();data.readString();data.readString();
     int status=data.readInt();data.readLong();data.readInt();data.readInt();data.readString();
     result.complete(status);
    }
    if(reply!=null)reply.writeNoException();return true;
   }
  };
  try(RemoteBinding binding=new RemoteBinding(context,"com.onyx.android.ksync","com.onyx.android.ksync.service.KSyncService")) {
   IBinder binder=binding.await();
   if(!DESCRIPTOR.equals(binder.getInterfaceDescriptor()))throw new IOException("Unexpected account Binder");
   Parcel p=Parcel.obtain();
   try {
    p.writeInterfaceToken(DESCRIPTOR);p.writeInt(1);
    p.writeString(restore ? null : "powersync-lab@example.invalid");p.writeString(restore ? null : "000000");p.writeString(null);p.writeInt(restore ? 21 : 1);
    p.writeString(null);p.writeString("PowerSync lab");p.writeString(null);p.writeString(null);
    p.writeInt(0);p.writeLong(0);p.writeInt(restore ? 0 : 2);p.writeInt(0);p.writeString(null);
    p.writeStrongBinder(callback);
    if(!binder.transact(65,p,null,IBinder.FLAG_ONEWAY))throw new IOException("Unsupported login transaction");
   }finally{p.recycle();}
   int status=result.get(60,TimeUnit.SECONDS);
   JSONObject after=new NativeClusterLab(context).snapshot();
   return new JSONObject().put("resultCode",status).put("success",status==0).put("after",after)
    .put("libraryIdentitiesAndStatusesPreserved",before.getJSONObject("library").getString("identityStatusSha256")
      .equals(after.getJSONObject("library").getString("identityStatusSha256")));
  }
 }
}
