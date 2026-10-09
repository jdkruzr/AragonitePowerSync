package dev.aragonite.powersync;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Only the pre-existing disposable Binder fixture may be changed here. */
final class NativeConflictLab {
 private static final String DOC="9e6004bb-7bc3-4a7e-87d9-79b9f9ba6206";
 private final Context context;
 NativeConflictLab(Context context){this.context=context;}
 JSONObject capture() throws Exception {
  JSONArray rows=new NativeNotes(context).notes("uniqueId=? AND type=? AND userId=?",new String[]{DOC,"1","powersync-lab"});
  if(rows.length()!=1)throw new IOException("Expected owned disposable conflict notebook");
  JSONObject row=rows.getJSONObject(0);
  if(!row.optString("title").startsWith("PowerSync fixture "))throw new IOException("Unexpected conflict fixture title");
  return row;
 }
 JSONObject change(String operation) throws Exception {
  if(!NativeClusterLab.LAB_ID.equals(new NativeSystemConfigProbe().read().getString("curClusterId")))throw new IOException("Requires lab cluster");
  JSONObject before=capture();NativeNotes notes=new NativeNotes(context);
  String expected=null;
  if("edit".equals(operation)){
   File request=new File(context.getFilesDir(),"conflict-request.json");if(request.length()>1024)throw new IOException("Oversized conflict request");
   expected=new JSONObject(new String(java.nio.file.Files.readAllBytes(request.toPath()),StandardCharsets.UTF_8)).getString("title");
   if(!expected.matches("PowerSync fixture conflict [A-Za-z0-9 _-]{1,80}"))throw new IOException("Invalid synthetic title");
   notes.send("RENAME_NOTE",new JSONObject().put("noteBean",new JSONObject().put("documentId",DOC).put("parentUniqueId",before.optString("parentUniqueId","")).put("title",expected)),false);
  }else if("remove".equals(operation)||"restore".equals(operation)){
   // TinyNoteModel is not the provider row schema: page-list blobs and SQL
   // date values cannot be passed through as its structured Java properties.
   JSONObject tiny=new JSONObject();
   for(String key:new String[]{"uniqueId","parentUniqueId","title","type","status","noteSyncStatus","pageCount","activeScene"})tiny.put(key,before.opt(key));
   notes.send("remove".equals(operation)?"DELETE_NOTE":"RESTORE_REMOVE_NOTE",new JSONObject().put("noteModelList",new JSONArray().put(tiny)),false);
  }else throw new IOException("Unsupported conflict operation");
  for(int i=0;i<80;i++){
   JSONObject row=capture();boolean done="edit".equals(operation)?expected.equals(row.optString("title")):row.optInt("status",-1)==("remove".equals(operation)?0:1);
   if(done)return new JSONObject().put("operation",operation).put("uid",android.os.Process.myUid()).put("before",before).put("after",row);
   Thread.sleep(250);
  }
  throw new IOException("Native conflict mutation not observed");
 }
}
