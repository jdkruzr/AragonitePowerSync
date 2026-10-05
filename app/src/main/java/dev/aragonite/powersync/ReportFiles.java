package dev.aragonite.powersync;

import android.content.Context;
import android.util.AtomicFile;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;

final class ReportFiles {
 static void write(Context context, String name, JSONObject report) throws Exception {
  AtomicFile file = new AtomicFile(new File(context.getFilesDir(),name));
  FileOutputStream output = null;
  try { output=file.startWrite(); output.write(report.toString(2).getBytes(StandardCharsets.UTF_8)); file.finishWrite(output); }
  catch(Exception e) { if(output != null) file.failWrite(output); throw e; }
 }
}
