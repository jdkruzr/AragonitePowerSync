package dev.aragonite.powersync;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Read/export only: original library ownership and sync flags are untouched. */
final class NativeLibraryLab {
 private final Context context;
 NativeLibraryLab(Context context){this.context=context;}
 JSONObject inventory() throws Exception {
  JSONArray rows=new NativeNotes(context).notes(null,null);int notes=0;
  JSONObject types=new JSONObject(),statuses=new JSONObject();
  for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);String t=""+row.optInt("type",-1),s=""+row.optInt("status",-1);types.put(t,types.optInt(t)+1);statuses.put(s,statuses.optInt(s)+1);if(row.optInt("type",-1)==1)notes++;}
  JSONObject report=new JSONObject().put("at",System.currentTimeMillis()).put("uid",android.os.Process.myUid()).put("metadataRows",rows.length()).put("notebooks",notes).put("types",types).put("statuses",statuses).put("rows",rows).put("payloadsExported",false);
  ReportFiles.write(context,"library-inventory-private.json",report);
  return new JSONObject().put("metadataRows",rows.length()).put("notebooks",notes).put("types",types).put("statuses",statuses).put("privateInventory","library-inventory-private.json").put("payloadsExported",false);
 }
 JSONObject exportOne() throws Exception {
  File request=new File(context.getFilesDir(),"library-export-request.json");
  if(request.length()>1024)throw new IOException("Oversized export request");
  String id=new JSONObject(new String(java.nio.file.Files.readAllBytes(request.toPath()),StandardCharsets.UTF_8)).getString("documentId");
  if(!id.matches("[0-9a-f-]{32,36}"))throw new IOException("Invalid document ID");
  JSONObject inventory=new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(context.getFilesDir(),"library-inventory-private.json").toPath()),StandardCharsets.UTF_8));
  boolean inventoried=false;JSONArray rows=inventory.getJSONArray("rows");for(int i=0;i<rows.length();i++)if(id.equals(rows.getJSONObject(i).optString("uniqueId"))&&rows.getJSONObject(i).optInt("type",-1)==1)inventoried=true;
  if(!inventoried)throw new IOException("Export requires inventoried notebook");
  JSONArray current=new NativeNotes(context).notes("uniqueId=? AND type=?",new String[]{id,"1"});if(current.length()!=1)throw new IOException("Expected one current notebook");
  return new FixtureRunner(context).exportRow(current.getJSONObject(0));
 }
}
