package dev.aragonite.powersync;

import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.net.Uri;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

final class ProbeRunner {
 private final Context context;
 private final NativeNotes notes;
 private final JSONObject report = new JSONObject();
 private final JSONArray checks = new JSONArray();
 ProbeRunner(Context context) { this.context = context; notes = new NativeNotes(context); }
 interface Check { Object run() throws Exception; }
 private void check(String name, Check fn) throws Exception {
  JSONObject r = new JSONObject().put("name", name);
  try { r.put("status", "pass").put("result", fn.run()); }
  catch (Exception e) { r.put("status", "fail").put("errorClass", e.getClass().getName()).put("message", String.valueOf(e.getMessage())); }
  checks.put(r); save();
 }
 JSONObject run() throws Exception {
  report.put("formatVersion", 1).put("createdAt", System.currentTimeMillis()).put("uid", android.os.Process.myUid())
   .put("model", Build.MODEL).put("sdk", Build.VERSION.SDK_INT).put("fingerprint", Build.FINGERPRINT)
   .put("allFilesAccess", Environment.isExternalStorageManager()).put("checks", checks);
  for (String pkg : new String[]{NativeNotes.NOTE_PACKAGE, "com.onyx.android.ksync"}) check(pkg + ".package", () -> {
   PackageInfo info = context.getPackageManager().getPackageInfo(pkg, PackageManager.GET_SERVICES | PackageManager.GET_PROVIDERS);
   JSONObject r = new JSONObject().put("versionCode", info.getLongVersionCode()).put("versionName", info.versionName);
   JSONArray services = new JSONArray();
   if (info.services != null) for (ServiceInfo s : info.services) if (s.name.endsWith(".NoteService") || s.name.endsWith(".KSyncService")) services.put(new JSONObject().put("name", s.name).put("exported",s.exported).put("permission",s.permission == null ? JSONObject.NULL : s.permission));
   return r.put("services",services);
  });
  check("notes.provider", () -> notes.rows(NativeNotes.NOTES, null, null, false));
  check("notes.shapeProvider", () -> notes.rows(Uri.parse("content://com.onyx.android.sdk.note.ContentProvider/ShapeModel"), null, null, false));
  check("notes.newShapeProvider", () -> notes.rows(Uri.parse("content://com.onyx.android.sdk.shape.ContentProvider/ShapeModel"), null, null, false));
  for (String[] s : new String[][]{{NativeNotes.NOTE_PACKAGE,NativeNotes.NOTE_SERVICE}, {"com.onyx.android.ksync","com.onyx.android.ksync.service.KSyncService"}}) check(s[1] + ".bind", () -> {
   try (RemoteBinding binding = new RemoteBinding(context,s[0],s[1])) { return new JSONObject().put("descriptor",binding.await().getInterfaceDescriptor()); }
  });
  check("notes.virtualPages", () -> {
   try (android.database.Cursor c = context.getContentResolver().query(NativeNotes.NOTES, new String[]{"uniqueId"}, "type=? AND status=?", new String[]{"1","1"}, "updatedAt DESC LIMIT 1")) {
    if (c == null || !c.moveToFirst()) return new JSONObject().put("available",false);
    return notes.pages(c.getString(0));
   }
  });
  check("storage.ksync", () -> {
   File root = new File(Environment.getExternalStorageDirectory(), ".ksync");
   File[] entries = root.listFiles();
   if (entries == null) throw new IOException("Cannot enumerate .ksync");
   JSONArray names = new JSONArray(); for (File f:entries) names.put(f.getName());
   return new JSONObject().put("entries",names);
  });
  save(); return report;
 }
 private void save() throws Exception { ReportFiles.write(context,"probe.json",report); }
}
