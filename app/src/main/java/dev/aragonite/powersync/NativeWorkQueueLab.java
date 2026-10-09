package dev.aragonite.powersync;

import android.content.*;
import android.net.Uri;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Synthetic V2 work only, bound to the existing disposable UI notebook. */
final class NativeWorkQueueLab {
 private final Context context;
 private static final String DOC="f7f635344f624650aa300f84d114a1d3";
 private static final Uri URI=Uri.parse("content://com.onyx.android.sdk.sync.KSyncRecordContentProvider/LocalRecordModel");
 NativeWorkQueueLab(Context context){this.context=context;}
 private JSONObject request() throws Exception {
  File file=new File(context.getFilesDir(),"v2-work-request.json");if(file.length()>2048)throw new IOException("Oversized V2 request");
  JSONObject r=new JSONObject(new String(java.nio.file.Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));
  if(!r.getString("recordId").matches("powersync-v2-[a-f0-9-]{36}"))throw new IOException("Invalid synthetic record ID");
  return r;
 }
 JSONObject capture() throws Exception {
  String record=request().getString("recordId");
  return new JSONObject().put("uid",android.os.Process.myUid()).put("records",new NativeNotes(context).rows(URI,"documentId=? AND user=? AND recordId=?",new String[]{DOC,"powersync-lab",record},true));
 }
 JSONObject insert() throws Exception {
  if(!NativeClusterLab.LAB_ID.equals(new NativeSystemConfigProbe().read().getString("curClusterId")))throw new IOException("Requires lab cluster");
  JSONObject req=request();String record=req.getString("recordId");
  JSONArray notes=new NativeNotes(context).notes("uniqueId=? AND userId=?",new String[]{DOC,"powersync-lab"});if(notes.length()!=1)throw new IOException("Missing owned disposable notebook");
  if(capture().getJSONArray("records").length()!=0)throw new IOException("Synthetic record already exists");
  String local="/sdcard/.ksync/document/"+DOC+"/resource/data/"+record+".html";if(new File(local).exists())throw new IOException("Synthetic destination already exists");
  int status=req.optInt("syncStatus",1);if(status!=1&&status!=3)throw new IOException("Only synthetic pull/delete work is supported");
  if(req.optBoolean("seedExisting")){
   File target=new File(local);if(!target.getParentFile().isDirectory()&&!target.getParentFile().mkdirs())throw new IOException("Cannot create synthetic directory");
   try(FileOutputStream out=new FileOutputStream(target)){out.write("PowerSync stable pre-replacement fixture".getBytes(StandardCharsets.UTF_8));out.getFD().sync();}
  }
  ContentValues v=new ContentValues();v.put("documentId",DOC);v.put("recordId",record);v.put("user","powersync-lab");v.put("recordType",4);v.put("syncStatus",status);v.put("recordFilePath",local);v.put("guid",record);v.put("idString",record);v.put("createdAt",System.currentTimeMillis());v.put("updatedAt",System.currentTimeMillis());
  Uri inserted=context.getContentResolver().insert(URI,v);if(inserted==null)throw new IOException("Queue insert failed");
  return capture().put("localPath",local).put("ossKey","powersync-lab/resource/"+record+".html");
 }
 JSONObject remove() throws Exception {
  String record=request().getString("recordId");int deleted=context.getContentResolver().delete(URI,"documentId=? AND user=? AND recordId=? AND guid=?",new String[]{DOC,"powersync-lab",record,record});return new JSONObject().put("deletedSyntheticRows",deleted);
 }
}
