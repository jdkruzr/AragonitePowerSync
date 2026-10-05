package dev.aragonite.powersync;

import android.content.*;
import android.os.*;
import java.util.concurrent.*;

/** Explicit binding with bounded lifetime. Bind/unbind must run on the main thread. */
final class RemoteBinding implements AutoCloseable {
 private final Context context;
 private final Handler main = new Handler(Looper.getMainLooper());
 private final CompletableFuture<IBinder> result = new CompletableFuture<>();
 private boolean bound;
 private final ServiceConnection connection = new ServiceConnection() {
  public void onServiceConnected(ComponentName name, IBinder binder) { result.complete(binder); }
  public void onServiceDisconnected(ComponentName name) { result.completeExceptionally(new IllegalStateException("service disconnected")); }
  public void onNullBinding(ComponentName name) { result.completeExceptionally(new IllegalStateException("null binding")); }
  public void onBindingDied(ComponentName name) { result.completeExceptionally(new IllegalStateException("binding died")); }
 };
 RemoteBinding(Context context, String pkg, String service) {
  this.context = context.getApplicationContext();
  main.post(() -> {
   try {
    bound = this.context.bindService(new Intent().setComponent(new ComponentName(pkg, service)), connection, Context.BIND_AUTO_CREATE);
    if (!bound) result.completeExceptionally(new IllegalStateException("bindService returned false"));
   } catch (Exception e) { result.completeExceptionally(e); }
  });
 }
 IBinder await() throws Exception { return result.get(15, TimeUnit.SECONDS); }
 public void close() { main.post(() -> { if (bound) { context.unbindService(connection); bound = false; } }); }
}
