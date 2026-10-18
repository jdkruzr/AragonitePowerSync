package dev.aragonite.powersync.client;

import java.util.ArrayDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class SingleOperationTest {
    private static class Queue implements Executor {
        final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
        public void execute(Runnable task) { tasks.add(task); }
        void next() { tasks.remove().run(); }
    }
    @Test public void duplicateTapsCannotQueueSecondMutation() {
        Queue worker = new Queue(), ui = new Queue();
        SingleOperation operation = new SingleOperation(worker, ui);
        AtomicInteger calls = new AtomicInteger();
        assertTrue(operation.start(calls::incrementAndGet, failure -> assertNull(failure)));
        assertFalse(operation.start(calls::incrementAndGet, failure -> fail()));
        worker.next(); assertTrue(operation.busy());
        assertFalse(operation.start(calls::incrementAndGet, failure -> fail()));
        ui.next(); assertFalse(operation.busy()); assertEquals(1, calls.get());
    }
    @Test public void completionCanDeliverToNewObserverWithoutRepeatingWork() {
        Queue worker = new Queue(), ui = new Queue(); SingleOperation operation = new SingleOperation(worker, ui);
        AtomicInteger oldScreen = new AtomicInteger(), newScreen = new AtomicInteger(), calls = new AtomicInteger();
        Runnable[] observer = {oldScreen::incrementAndGet};
        operation.start(calls::incrementAndGet, failure -> observer[0].run());
        observer[0] = newScreen::incrementAndGet;
        worker.next(); ui.next();
        assertEquals(1, calls.get()); assertEquals(0, oldScreen.get()); assertEquals(1, newScreen.get());
    }
    @Test public void failureReleasesOperationAndIsDelivered() {
        Queue worker = new Queue(), ui = new Queue(); SingleOperation operation = new SingleOperation(worker, ui);
        operation.start(() -> { throw new Exception("synthetic"); }, failure -> assertEquals("synthetic", failure.getMessage()));
        worker.next(); ui.next(); assertFalse(operation.busy());
        assertTrue(operation.start(() -> {}, failure -> assertNull(failure)));
        worker.next(); ui.next();
    }
}
