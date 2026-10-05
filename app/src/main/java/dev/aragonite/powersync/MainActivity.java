package dev.aragonite.powersync;

import android.app.Activity;
import android.os.*;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.widget.*;
import java.util.concurrent.*;
import org.json.*;

public final class MainActivity extends Activity {
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private TextView output;
 protected void onCreate(Bundle state) {
  super.onCreate(state);
  LinearLayout layout=new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(24,24,24,24);
  TextView heading=new TextView(this); heading.setText("PowerSync — native adapter lab"); heading.setTextSize(24); layout.addView(heading);
  TextView intro=new TextView(this); intro.setText("Read-only probes run as this app. Fixture commands affect only a disposable note created by PowerSync. Full sync is not enabled."); layout.addView(intro);
  add(layout,"Probe native interfaces",() -> new ProbeRunner(this).run(),"probe.json");
  Button storage=new Button(this); storage.setText("Grant All Files Access"); storage.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,Uri.parse("package:"+getPackageName())))); layout.addView(storage);
  add(layout,"Create disposable text fixture",() -> new FixtureRunner(this).create(),"fixture-create.json");
  add(layout,"Rename fixture in place",() -> new FixtureRunner(this).rename(),"fixture-rename.json");
  add(layout,"Capture fixture metadata",() -> new FixtureRunner(this).capture(),"fixture-capture.json");
  add(layout,"Export native fixture archive",() -> new FixtureRunner(this).export(),"fixture-export.json");
  add(layout,"Write fixture text resource (close Notes first)",() -> new FixtureRunner(this).writeTextResource(),"fixture-write.json");
  Button open=new Button(this); open.setText("Open fixture in BOOX Notes"); open.setOnClickListener(v -> worker.execute(() -> {try{Intent intent=new FixtureRunner(this).open();runOnUiThread(() -> startActivity(intent));}catch(Exception e){show(e.toString());}})); layout.addView(open);
  ScrollView scroll=new ScrollView(this); output=new TextView(this); output.setTextIsSelectable(true); scroll.addView(output); layout.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(layout);
  handleDebugIntent(getIntent());
 }
 protected void onNewIntent(Intent intent) { super.onNewIntent(intent);setIntent(intent);handleDebugIntent(intent); }
 private void handleDebugIntent(Intent intent) {
  String action=intent.getStringExtra("lab_action");
  if(!BuildConfig.DEBUG || action == null) return;
  switch(action) {
   case "cluster_snapshot" -> execute(() -> new NativeClusterLab(this).snapshot(),"cluster-snapshot.json");
   case "cluster_baseline" -> execute(() -> new NativeClusterLab(this).baseline(),"cluster-baseline-result.json");
   case "cluster_update" -> execute(() -> new NativeClusterLab(this).update(),"cluster-update.json");
   case "cluster_switch" -> execute(() -> new NativeClusterLab(this).change(false),"cluster-switch.json");
   case "cluster_restore" -> execute(() -> new NativeClusterLab(this).change(true),"cluster-restore.json");
   case "probe" -> execute(() -> new ProbeRunner(this).run(),"probe.json");
   case "create_fixture" -> execute(() -> new FixtureRunner(this).create(),"fixture-create.json");
   case "rename_fixture" -> execute(() -> new FixtureRunner(this).rename(),"fixture-rename.json");
   case "capture_fixture" -> execute(() -> new FixtureRunner(this).capture(),"fixture-capture.json");
   case "export_fixture" -> execute(() -> new FixtureRunner(this).export(),"fixture-export.json");
   case "disable_fixture" -> execute(() -> new FixtureRunner(this).disableNativeCloud(),"fixture-cloud.json");
   case "disable_ink" -> execute(() -> new FixtureRunner(this,"inkFixture").disableNativeCloud(),"ink-cloud.json");
   case "write_fixture" -> execute(() -> new FixtureRunner(this).writeTextResource(),"fixture-write.json");
   case "image_ink" -> worker.execute(() -> {try{Intent open=new FixtureRunner(this,"inkFixture").insertImage();runOnUiThread(() -> startActivity(open));}catch(Exception e){show(e.toString());}});
   case "create_ink" -> worker.execute(() -> {try{Intent open=new FixtureRunner(this,"inkFixture").createInkIntent();runOnUiThread(() -> startActivity(open));}catch(Exception e){show(e.toString());}});
   case "capture_ink" -> execute(() -> new FixtureRunner(this,"inkFixture").capture(),"ink-metadata.json");
   case "export_ink" -> execute(() -> new FixtureRunner(this,"inkFixture").export(),"ink-export.json");
   case "stage_ink_interrupt" -> execute(() -> new FixtureRunner(this,"inkFixture").stageInkPayload(true),"ink-stage.json");
   case "stage_ink" -> execute(() -> new FixtureRunner(this,"inkFixture").stageInkPayload(),"ink-stage.json");
   case "open_ink" -> worker.execute(() -> {try{Intent open=new FixtureRunner(this,"inkFixture").open();runOnUiThread(() -> startActivity(open));}catch(Exception e){show(e.toString());}});
   case "open_fixture" -> worker.execute(() -> {try{Intent open=new FixtureRunner(this).open();runOnUiThread(() -> startActivity(open));}catch(Exception e){show(e.toString());}});
  }
 }
 interface Task { JSONObject run() throws Exception; }
 private void add(LinearLayout layout,String label,Task task,String report) { Button button=new Button(this);button.setText(label);button.setOnClickListener(v -> execute(task,report));layout.addView(button); }
 private void execute(Task task,String report) { show("Running…");worker.execute(() -> {try {JSONObject result=task.run();ReportFiles.write(this,report,result);show(result.toString(2));}catch(Exception e){try{JSONObject failure=new JSONObject().put("error",e.toString());ReportFiles.write(this,report,failure);}catch(Exception ignored){}show(e.toString());}}); }
 private void show(String text) {runOnUiThread(() -> {if(!isFinishing())output.setText(text);});}
 protected void onDestroy() {worker.shutdown();super.onDestroy();}
}
