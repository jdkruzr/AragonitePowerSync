package dev.aragonite.powersync;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import com.onyx.android.sdk.note.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

final class NativeNotes {
 static final String NOTE_PACKAGE = "com.onyx.android.note";
 static final String NOTE_SERVICE = NOTE_PACKAGE + ".note.NoteService";
 static final Uri NOTES = Uri.parse("content://com.onyx.android.sdk.note.ContentProvider/NoteModel");
 private final Context context;
 NativeNotes(Context context) { this.context = context; }
 JSONArray notes(String selection, String[] args) throws Exception {
  return rows(NOTES, selection, args, true);
 }
 JSONArray rows(Uri uri, String selection, String[] args, boolean complete) throws Exception {
  JSONArray out = new JSONArray();
  try (Cursor c = context.getContentResolver().query(uri, null, selection, args, null)) {
   if (c == null) throw new IOException("Provider returned null cursor");
   if (!complete) return new JSONArray().put(new JSONObject().put("count", c.getCount()).put("columns", new JSONArray(c.getColumnNames())));
   while (c.moveToNext()) {
    JSONObject row = new JSONObject();
    for (int i=0; i<c.getColumnCount(); i++) {
     Object value = switch (c.getType(i)) {
      case Cursor.FIELD_TYPE_NULL -> JSONObject.NULL;
      case Cursor.FIELD_TYPE_INTEGER -> c.getLong(i);
      case Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i);
      case Cursor.FIELD_TYPE_BLOB -> new JSONObject().put("encoding", "base64").put("value", android.util.Base64.encodeToString(c.getBlob(i), android.util.Base64.NO_WRAP));
      case Cursor.FIELD_TYPE_STRING -> c.getString(i);
      default -> throw new IOException("Unexpected cursor value type");
     };
     row.put(c.getColumnName(i), value);
    }
    out.put(row);
   }
  }
  return out;
 }
 JSONObject pages(String docId) throws Exception {
  try (RemoteBinding binding = new RemoteBinding(context, NOTE_PACKAGE, NOTE_SERVICE)) {
   IBinder binder = binding.await();
   if (!"com.onyx.android.sdk.note.INoteContentService".equals(binder.getInterfaceDescriptor())) throw new IOException("Unexpected Notes interface");
   CompletableFuture<JSONObject> result = new CompletableFuture<>();
   INoteContentService.Stub.asInterface(binder).loadVirtualPageIdList(docId, new NoteContentIntentCallback.Stub() {
    public void read(NoteContentIntentArgs args) {
     try {
      if (args == null || !docId.equals(args.docId)) throw new IOException("Unexpected callback document");
      String content = args.content;
      if (args.pfd != null) {
       try (InputStream stream = new ParcelFileDescriptor.AutoCloseInputStream(args.pfd)) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192]; int n;
        while ((n = stream.read(chunk)) != -1) { if (buffer.size()+n > 4*1024*1024) throw new IOException("Page callback exceeds bound"); buffer.write(chunk,0,n); }
        content = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
       }
      }
      result.complete(new JSONObject().put("documentId", docId).put("pageIds", new JSONArray(content)));
     } catch (Exception e) { result.completeExceptionally(e); }
    }
   });
   return result.get(20, TimeUnit.SECONDS);
  }
 }
 void send(String action, JSONObject bean, boolean direct) throws Exception {
  try (RemoteBinding binding = new RemoteBinding(context, NOTE_PACKAGE, NOTE_SERVICE)) {
   IBinder binder = binding.await();
   if (!"com.onyx.android.sdk.note.INoteContentService".equals(binder.getInterfaceDescriptor())) throw new IOException("Unexpected Notes interface");
   Bundle command = new Bundle(); command.putString("SERVICE_ACTION", action);
   command.putString(direct ? "OPEN_NOTE_BEAN" : "note_service_bean", bean.toString());
   INoteContentService.Stub.asInterface(binder).sendMessageAction(command);
  }
 }
 Intent openIntent(JSONObject row) throws Exception {
  JSONObject bean = new JSONObject().put("documentId", row.getString("uniqueId"))
   .put("parentUniqueId", row.optString("parentUniqueId", ""))
   .put("title", row.optString("title", "")).put("activeScene", row.optInt("activeScene", 0));
  return new Intent().setComponent(new ComponentName(NOTE_PACKAGE, NOTE_PACKAGE + ".note.ui.ScribbleActivity"))
   .putExtra("OPEN_NOTE_BEAN", bean.toString()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
 }
 void notifyFixtureData(String documentId) throws Exception {
  try(RemoteBinding binding=new RemoteBinding(context,NOTE_PACKAGE,NOTE_SERVICE)) {
   IBinder binder=binding.await();
   if(!"com.onyx.android.sdk.note.INoteContentService".equals(binder.getInterfaceDescriptor())) throw new IOException("Unexpected Notes interface");
   Bundle command=new Bundle();
   command.putString("KSYNC_NOTE_ACTION","KSYNC_NOTE_DATA_UPDATE_ACTION");
   command.putString("KEY_DOCUMENT_ID",documentId);
   INoteContentService.Stub.asInterface(binder).sendMessageAction(command);
  }
 }
}
