package dev.aragonite.powersync.client;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/** One application-owned operation; changing observers cannot restart it. */
final class SingleOperation {
    interface Work { void run() throws Exception; }
    interface Completion { void finish(Exception failure); }
    private final AtomicBoolean busy = new AtomicBoolean();
    private final Executor worker, delivery;
    SingleOperation(Executor worker, Executor delivery) { this.worker = worker; this.delivery = delivery; }
    boolean busy() { return busy.get(); }
    boolean start(Work work, Completion completion) {
        if (!busy.compareAndSet(false, true)) return false;
        worker.execute(() -> {
            Exception failure = null;
            try { work.run(); } catch (Exception e) { failure = e; }
            Exception result = failure;
            delivery.execute(() -> { busy.set(false); completion.finish(result); });
        });
        return true;
    }
}
