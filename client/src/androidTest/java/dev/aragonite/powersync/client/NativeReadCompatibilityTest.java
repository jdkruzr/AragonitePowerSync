package dev.aragonite.powersync.client;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.app.Instrumentation;
import android.app.Activity;
import android.os.Bundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Read-only device check, running as the product UID. No enrollment or writes. */
public class NativeReadCompatibilityTest extends Instrumentation {
    private boolean enrollmentDiagnostic;
    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        enrollmentDiagnostic = arguments != null && "true".equals(arguments.getString("enrollmentDiagnostic"));
        start();
    }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            if (enrollmentDiagnostic) {
                result.putString("stream", enrollmentDiagnostic());
                finish(Activity.RESULT_OK, result);
                return;
            }
            testNativeReads();
            testNativeServiceDescriptor();
            result.putString("stream", "Native reads and service descriptor: 2 checks passed\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            // Avoid dumping provider replies or account values into host logs.
            result.putString("stream", "Native compatibility check failed: " + failure.getClass().getSimpleName() + "\n");
            finish(Activity.RESULT_CANCELED, result);
        }
    }
    /** Read only; emit aggregate facts, never saved credentials or provider rows. */
    private String enrollmentDiagnostic() throws Exception {
        TransitionController controller = new TransitionController(getTargetContext());
        org.json.JSONObject status = controller.status();
        org.json.JSONObject baseline = controller.state.read("baseline");
        org.json.JSONObject journal = controller.state.read("transition");
        StringBuilder report = new StringBuilder();
        for (String key : new String[]{"phase", "hasBaseline", "available", "accountLoggedIn", "identityMatches", "configurationMatches", "allFilesAccess", "flags"})
            report.append(key).append('=').append(status.opt(key)).append('\n');
        if (journal != null) report.append("transitionUpdatedAt=").append(journal.optLong("updatedAt")).append('\n');
        if (baseline == null) return report.toString();
        report.append("notebookFingerprintMatches=").append(baseline.getString("fingerprint").equals(controller.nativeIO.fingerprint())).append('\n');
        org.json.JSONArray before = baseline.getJSONArray("catalog"), now = controller.nativeIO.catalog();
        java.util.Set<String> present = new java.util.HashSet<>();
        for (int i = 0; i < now.length(); i++) present.add(now.getJSONObject(i).getString("uuid"));
        report.append("baselineCatalogDigest=").append(NativeBridge.hex(java.security.MessageDigest.getInstance("SHA-256").digest(NativeBridge.canonical(before).getBytes(java.nio.charset.StandardCharsets.UTF_8)))).append('\n');
        java.util.Map<Long, String> currentIds = new java.util.HashMap<>();
        for (int i = 0; i < now.length(); i++) currentIds.put(now.getJSONObject(i).getLong("id"), now.getJSONObject(i).getString("uuid"));
        int missing = 0, missingFile = 0, emptyPath = 0, cloudReferences = 0, fallbackFiles = 0, conflicts = 0;
        java.util.Map<Integer, Integer> sources = new java.util.TreeMap<>();
        for (int i = 0; i < before.length(); i++) {
            org.json.JSONObject row = before.getJSONObject(i);
            if (present.contains(row.getString("uuid"))) continue;
            missing++;
            if (currentIds.containsKey(row.getLong("id"))) conflicts++;
            if (!row.optString("cloudId").isEmpty() || !row.optString("uniqueCloudId").isEmpty()) cloudReferences++;
            if (new java.io.File(row.optString("nocasePath")).isFile()) fallbackFiles++;
            int source = row.optInt("fetchSource");
            sources.put(source, sources.getOrDefault(source, 0) + 1);
            String path = row.optString("nativeAbsolutePath", "");
            boolean exists = new java.io.File(path).isFile();
            if (!exists) missingFile++;
            if (path.isEmpty()) emptyPath++;

        }
        report.append("baselineBooks=").append(before.length()).append("; currentBooks=").append(now.length())
            .append("; missingRows=").append(missing).append("; missingFiles=").append(missingFile)
            .append("; emptyPaths=").append(emptyPath).append("; occupiedMissingIds=").append(conflicts).append('\n');
        report.append("missingRowsWithCloudReferences=").append(cloudReferences)
            .append("; existingFallbackPaths=").append(fallbackFiles)
            .append("; fetchSourceCounts=").append(sources).append('\n');
        return report.toString();
    }

    private static void assertTrue(boolean value) { if (!value) throw new AssertionError(); }
    private static void assertNotNull(Object value) { assertTrue(value != null); }
    private static void assertEquals(Object expected, Object actual) { assertTrue(expected.equals(actual)); }

    public void testNativeReads() throws Exception {
        Context context = getTargetContext();
        NativeBridge bridge = new NativeBridge(context);
        bridge.requireKsyncAvailable();
        assertNotNull(bridge.read(NativeBridge.CLUSTER).getJSONObject("clusterMap"));
        assertNotNull(bridge.read(NativeBridge.SYSTEM).getJSONObject("configMap"));
        assertTrue(bridge.flags().has("notes"));
        assertTrue(bridge.account().has("loggedIn"));
        assertEquals(64, bridge.fingerprint().length());
        assertNotNull(bridge.catalog());
        assertNotNull(new NativeAccountBackup(bridge).capture(bridge.account().optString("uid")));
        assertTrue(new TransitionController(context).status().getBoolean("available"));
    }

    public void testNativeServiceDescriptor() throws Exception {
        Context context = getTargetContext();
        CompletableFuture<IBinder> binder = new CompletableFuture<>();
        ServiceConnection connection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) { binder.complete(service); }
            public void onServiceDisconnected(ComponentName name) {}
            public void onNullBinding(ComponentName name) { binder.completeExceptionally(new IllegalStateException("Null native binding")); }
        };
        boolean bound = context.bindService(new Intent().setComponent(new ComponentName(
            "com.onyx.android.ksync", "com.onyx.android.ksync.service.KSyncService")), connection, Context.BIND_AUTO_CREATE);
        assertTrue(bound);
        try { assertEquals(NativeBridge.SERVICE, binder.get(15, TimeUnit.SECONDS).getInterfaceDescriptor()); }
        finally { context.unbindService(connection); }
    }
}
