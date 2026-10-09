package dev.aragonite.powersync;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/** Delayed, request-only ordinary-UID probe for the fixed disposable editor. */
final class NativeEditorCloseLab {
 private final Context context;
 NativeEditorCloseLab(Context context) { this.context=context.getApplicationContext(); }
 private void receipt(String name, JSONObject value) throws Exception {
  try(FileOutputStream out=new FileOutputStream(new File(context.getFilesDir(),name))) {
   out.write(value.toString(2).getBytes(StandardCharsets.UTF_8));out.getFD().sync();
  }
 }
 JSONObject arm(boolean quit) throws Exception {
  NativeGatewayLab lab=new NativeGatewayLab(context);
  Intent open=lab.selectedOpen(); // Existing lab-cluster and selected-fixture guards.
  String id=lab.selectedCapture().getJSONObject("note").getString("uniqueId");
  if(!"f7f635344f624650aa300f84d114a1d3".equals(id))throw new java.io.IOException("Fixed synthetic notebook only");
  String name=quit?"editor-quit-dispatch.json":"editor-noop-dispatch.json";
  new File(context.getFilesDir(),name).delete();
  long armed=SystemClock.elapsedRealtime();
  new Handler(Looper.getMainLooper()).postDelayed(() -> {
   try {
    JSONObject event=new JSONObject().put("uid",android.os.Process.myUid()).put("documentId",id)
     .put("armedElapsedMs",armed).put("dispatchElapsedMs",SystemClock.elapsedRealtime())
     .put("dispatchEpochMs",System.currentTimeMillis()).put("quitRequested",quit).put("completed",false)
     .put("editorIdentityAtDispatchVerified",false);
    if(quit)context.sendBroadcast(new Intent("com.onyx.action.QUIT_NOTE").setPackage(NativeNotes.NOTE_PACKAGE));
    receipt(name,event);
   } catch(Exception e) { try {receipt(name,new JSONObject().put("error",e.toString()));}catch(Exception ignored){} }
  },45000);
  context.startActivity(open);
  return new JSONObject().put("armed",true).put("delayMs",45000).put("quit",quit).put("completed",false);
 }
 JSONObject switchArm(boolean different) throws Exception {
  NativeGatewayLab lab=new NativeGatewayLab(context);
  Intent source=lab.selectedOpen();
  JSONObject selected=lab.selectedCapture().getJSONObject("note");
  String id=selected.getString("uniqueId");
  if(!"f7f635344f624650aa300f84d114a1d3".equals(id)||!"powersync-lab".equals(selected.optString("userId")))throw new java.io.IOException("Owned fixed source only");
  String destination=different?"9e6004bb-7bc3-4a7e-87d9-79b9f9ba6206":id;
  org.json.JSONArray rows=lab.sharedCapture().getJSONArray("notes");JSONObject target=null;
  for(int i=0;i<rows.length();i++)if(destination.equals(rows.getJSONObject(i).getString("uniqueId")))target=rows.getJSONObject(i);
  if(target==null||target.optInt("status")!=1||target.optInt("type")!=1)throw new java.io.IOException("Live allowlisted lab destination required");
  Intent next=new NativeNotes(context).openIntent(target);
  String name=different?"editor-switch-dispatch.json":"editor-same-dispatch.json";
  new File(context.getFilesDir(),name).delete();long armed=SystemClock.elapsedRealtime();
  new Handler(Looper.getMainLooper()).postDelayed(() -> {
   try {
    // Revalidate cluster and owned source without foregrounding our activity.
    lab.selectedOpen();
    context.startActivity(next);
    receipt(name,new JSONObject().put("uid",android.os.Process.myUid()).put("documentId",id)
     .put("destinationId",destination).put("different",different).put("armedElapsedMs",armed)
     .put("dispatchElapsedMs",SystemClock.elapsedRealtime()).put("dispatchEpochMs",System.currentTimeMillis())
     .put("completed",false).put("editorIdentityAtDispatchVerified",false));
   }catch(Exception e){try{receipt(name,new JSONObject().put("error",e.toString()));}catch(Exception ignored){}}
  },45000);
  context.startActivity(source);
  return new JSONObject().put("armed",true).put("delayMs",45000).put("destinationId",destination).put("completed",false);
 }

 JSONObject switchForeground() throws Exception {
  NativeGatewayLab lab=new NativeGatewayLab(context);lab.selectedOpen();
  JSONObject source=lab.selectedCapture().getJSONObject("note");
  if(!"f7f635344f624650aa300f84d114a1d3".equals(source.getString("uniqueId"))||!"powersync-lab".equals(source.optString("userId")))throw new java.io.IOException("Owned source required");
  String id="9e6004bb-7bc3-4a7e-87d9-79b9f9ba6206";
  org.json.JSONArray rows=lab.sharedCapture().getJSONArray("notes");JSONObject target=null;
  for(int i=0;i<rows.length();i++)if(id.equals(rows.getJSONObject(i).getString("uniqueId")))target=rows.getJSONObject(i);
  if(target==null||target.optInt("status")!=1||target.optInt("type")!=1)throw new java.io.IOException("Live allowlisted destination required");
  context.startActivity(new NativeNotes(context).openIntent(target));
  return new JSONObject().put("uid",android.os.Process.myUid()).put("destinationId",id)
   .put("dispatchEpochMs",System.currentTimeMillis()).put("explicitHelperForegroundTransition",true).put("completed",false);
 }

}
