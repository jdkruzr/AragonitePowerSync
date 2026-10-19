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
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
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
    private static void assertTrue(boolean value) { if (!value) throw new AssertionError(); }
    private static void assertNotNull(Object value) { assertTrue(value != null); }
    private static void assertEquals(Object expected, Object actual) { assertTrue(expected.equals(actual)); }

    public void testNativeReads() throws Exception {
        Context context = getTargetContext();
        NativeBridge bridge = new NativeBridge(context);
        bridge.compatible();
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
