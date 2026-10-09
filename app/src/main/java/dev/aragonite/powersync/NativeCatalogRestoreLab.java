package dev.aragonite.powersync;
import android.content.*;import android.net.Uri;import org.json.*;import java.io.*;import java.nio.file.*;
/** Single catalog-row repair after the explicitly authorized original-endpoint restoration. */
final class NativeCatalogRestoreLab {
 static JSONObject wake(Context context)throws Exception {
  if(!"en".equals(new NativeSystemConfigProbe().read().getString("curClusterId")))throw new IOException("Original endpoint required");
  try(RemoteBinding binding=new RemoteBinding(context,"com.onyx.android.ksync","com.onyx.android.ksync.service.KSyncService")){if(!"com.onyx.android.sdk.sync.ksync.service.ISyncService".equals(binding.await().getInterfaceDescriptor()))throw new IOException("Unexpected native interface");context.sendBroadcast(new Intent("com.onyx.action.system.config.changed"));}
  return new JSONObject().put("nativeServiceStarted",true).put("originalEndpointVerified",true);
 }
 static JSONObject restore(Context context)throws Exception {
  if(!"Palma2_Pro_C".equals(android.os.Build.MODEL))throw new IOException("Qualified Palma restoration only");
  NativeSystemConfigProbe io=new NativeSystemConfigProbe();if(!"en".equals(io.read().getString("curClusterId")))throw new IOException("Original endpoint required");
  JSONObject flags=io.read("ONYX_SYSTEM_CONFIG_KEY").getJSONObject("configMap");if(!"false".equals(flags.getString("key_enable_sync_note_data"))||!"false".equals(flags.getString("key_enable_sync_reader_data")))throw new IOException("Sync must remain paused");
  byte[] raw=Files.readAllBytes(new File(context.getFilesDir(),"catalog-restore-private.json").toPath());if(!"1a76450d1eac2e3a6134ac86d30d6ab8f70fa25a5c7b7a96190d3f03f9510cf8".equals(NativeInk.digest(raw)))throw new IOException("Exact escrowed pre-transition row required");
  JSONObject row=new JSONObject(new String(raw,java.nio.charset.StandardCharsets.UTF_8));String uuid="dd9c0167293c411b9df712f89ff98af6";if(!uuid.equals(row.getString("uuid")))throw new IOException("Unexpected catalog identity");
  Uri uri=Uri.parse("content://com.onyx.content.database.ContentProvider/Metadata");NativeNotes notes=new NativeNotes(context);JSONArray existing=notes.rows(uri,"uuid=?",new String[]{uuid},true);
  if(existing.length()!=0)throw new IOException("Catalog row already present; do not overwrite");
  if(notes.rows(uri,"id=?",new String[]{row.getString("id")},true).length()!=0)throw new IOException("Original row ID reused");
  if(!new File(row.getString("nativeAbsolutePath")).isFile())throw new IOException("Original backing book missing");
  ContentValues values=new ContentValues();java.util.Iterator<String> keys=row.keys();while(keys.hasNext()){String k=keys.next();Object v=row.get(k);if(v==JSONObject.NULL)values.putNull(k);else if(v instanceof Integer||v instanceof Long)values.put(k,((Number)v).longValue());else if(v instanceof Number)values.put(k,((Number)v).doubleValue());else if(v instanceof String)values.put(k,(String)v);else throw new IOException("Unsupported value");}
  if(context.getContentResolver().insert(uri,values)==null)throw new IOException("Native catalog insert failed");JSONArray after=notes.rows(uri,"uuid=?",new String[]{uuid},true);if(after.length()!=1)throw new IOException("Catalog identity not restored");JSONObject restored=after.getJSONObject(0);keys=row.keys();while(keys.hasNext()){String k=keys.next();if(!String.valueOf(row.get(k)).equals(String.valueOf(restored.get(k))))throw new IOException("Restored catalog field differs: "+k);}
  return new JSONObject().put("singleOriginalCatalogRowRestoredExact",true).put("bookFileUntouched",true).put("notesUntouched",true);
 }
}
