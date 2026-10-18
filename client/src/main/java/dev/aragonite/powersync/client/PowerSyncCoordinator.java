package dev.aragonite.powersync.client;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Process-owned work and results. Activities observe; no worker captures an Activity. */
final class PowerSyncCoordinator {
    interface Observer { void changed(); }
    interface Task { void run() throws Exception; }
    static final class Prompt {
        final String kind;
        final Object data;
        Prompt(String kind, Object data) { this.kind = kind; this.data = data; }
    }
    // Only the application context is retained; Activities detach their observer in onStop.
    @android.annotation.SuppressLint("StaticFieldLeak")
    private static PowerSyncCoordinator instance;
    static synchronized PowerSyncCoordinator get(Context context) {
        if (instance == null) instance = new PowerSyncCoordinator(context.getApplicationContext());
        return instance;
    }
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ExecutorService checks = Executors.newSingleThreadExecutor();
    private final SingleOperation operation = new SingleOperation(worker, r -> main.post(r));
    private final TransitionController controller;
    private Observer observer;
    ConnectionState connection;
    String message = "", statusError = "", endpointError = "";
    int progressResource;
    private String locale = "";
    String pendingServer = "", draftServer = "", draftCode = "";
    boolean hasDraft, needsFreshCode, checking;
    int endpointChanges;
    private long lastCheck = -1, generation;
    private Prompt prompt;

    private PowerSyncCoordinator(Context context) {
        this.context = context;
        controller = new TransitionController(context);
        controller.progress = resource -> main.post(() -> { progressResource = resource; changed(); });
    }
    void localeChanged(String languageTags) {
        if (!locale.equals(languageTags)) {
            locale = languageTags;
            message = ""; statusError = ""; endpointError = "";
        }
    }
    void observe(Observer next) { observer = next; changed(); }
    void detach(Observer previous) { if (observer == previous) observer = null; }
    boolean busy() { return operation.busy(); }
    private String text(int id) { return context.getString(id); }
    private void changed() { if (observer != null) observer.changed(); }
    Prompt takePrompt() { Prompt result = prompt; prompt = null; return result; }
    void retainPrompt(Prompt value) { if (prompt == null) prompt = value; }
    void showMessage(String value) { message = value; changed(); }

    private void readStatus() {
        try { connection = new ConnectionState(controller.status()); statusError = ""; }
        catch (Exception e) {
            statusError = error(e);
            try { connection = new ConnectionState(controller.localStatus()); }
            catch (Exception ignored) { connection = new ConnectionState(new JSONObject()); }
        }
        try {
            JSONObject pending = controller.state.read("pending-setup");
            hasDraft = pending != null && "draft".equals(pending.optString("stage"));
            needsFreshCode = pending != null && !hasDraft && connection.screen != ConnectionState.Screen.ENROLLED;
            pendingServer = pending == null ? "" : pending.optString("server");
        } catch (Exception e) { statusError = error(e); hasDraft = false; needsFreshCode = false; }
    }
    private void run(int progressId, Task task, boolean checkAfter) {
        if (busy()) return;
        generation++;
        progressResource = progressId;
        operation.start(() -> {
            try { task.run(); } finally { readStatus(); }
        }, failure -> {
            if (failure != null) { prompt = null; message = error(failure); }
            progressResource = 0;
            if (connection == null || connection.screen != ConnectionState.Screen.CONNECTED) endpointChanges = 0;
            changed();
            if (checkAfter && observer != null) checkEndpoints(false);
        });
        changed();
    }
    void refresh() {
        run(R.string.progress_checking, () -> {}, true);
    }
    void prepare(String server, String code) {
        run(R.string.progress_checking, () -> {
            controller.nativeIO.compatible();
            ConnectionState current = new ConnectionState(controller.status());
            if (current.screen != ConnectionState.Screen.SETUP) throw new UserError(R.string.error_connection_changed);
            String base = SafeURL.base(server);
            String secret = code.trim();
            if (secret.isEmpty()) throw new UserError(R.string.error_enter_code);
            controller.state.write("pending-setup", new JSONObject().put("stage", "draft").put("server", base).put("code", secret));
            prompt = new Prompt(Environment.isExternalStorageManager() ? "connect" : "permission", base);
            message = "";
        }, false);
    }
    void continueSetup() {
        run(R.string.progress_checking, () -> {
            ConnectionState current = new ConnectionState(controller.status());
            if (current.screen != ConnectionState.Screen.SETUP && current.screen != ConnectionState.Screen.ENROLLED) throw new UserError(R.string.error_connection_changed);
            JSONObject pending = controller.state.read("pending-setup");
            String target = current.screen == ConnectionState.Screen.ENROLLED ? current.server : pending == null ? "" : pending.optString("server");
            if (target.isEmpty()) throw new UserError(R.string.error_enter_code);
            prompt = new Prompt(Environment.isExternalStorageManager() ? "connect" : "permission", target);
        }, false);
    }
    void permissionReturned() {
        run(R.string.progress_checking, () -> {
            if (!Environment.isExternalStorageManager()) return;
            ConnectionState current = new ConnectionState(controller.status());
            JSONObject pending = controller.state.read("pending-setup");
            if (current.screen == ConnectionState.Screen.ENROLLED) prompt = new Prompt("connect", current.server);
            else if (current.screen == ConnectionState.Screen.SETUP && pending != null && "draft".equals(pending.optString("stage"))) {
                prompt = new Prompt("connect", pending.getString("server"));
            }
        }, true);
    }
    void cancelSetup() {
        run(R.string.progress_checking, () -> { controller.state.delete("pending-setup"); message = ""; }, false);
    }
    void changeSetup() {
        run(R.string.progress_checking, () -> {
            ConnectionState current = new ConnectionState(controller.status());
            if (current.screen != ConnectionState.Screen.SETUP && current.screen != ConnectionState.Screen.ENROLLED) throw new UserError(R.string.error_connection_changed);
            controller.state.delete("pending-setup");
            controller.state.delete("profile");
            message = "";
        }, false);
    }
    void connect() {
        run(R.string.progress_enrolling, () -> {
            if (!Environment.isExternalStorageManager()) throw new UserError(R.string.error_grant_all_files_access_before_configuring_or);
            SetupFlow flow = new SetupFlow(new SetupFlow.Store() {
                public JSONObject pending() throws Exception { return controller.state.read("pending-setup"); }
                public void save(JSONObject value) throws Exception { controller.state.write("pending-setup", value); }
                public void clear() { controller.state.delete("pending-setup"); }
            }, new SetupFlow.Connection() {
                public boolean enrolled() throws Exception { return new ConnectionState(controller.status()).screen == ConnectionState.Screen.ENROLLED; }
                public void enroll(String server, String code) throws Exception { controller.enroll(server, code); }
                public void connect() throws Exception { controller.connect(); }
            });
            try { flow.confirm(); } catch (SetupFlow.FreshCodeRequired e) { throw new UserError(R.string.error_fresh_code); }
            message = text(R.string.connection_complete);
        }, true);
    }
    void enableSync() { run(R.string.progress_enabling, () -> { controller.enableSync(); message = text(R.string.sync_enabled); }, false); }
    void restore() {
        run(R.string.progress_restoring, () -> {
            controller.restore(); controller.state.delete("pending-setup");
            message = text(R.string.restore_complete);
        }, false);
    }
    void checkEndpoints(boolean manual) {
        if (busy() || checking || connection == null || connection.screen != ConnectionState.Screen.CONNECTED || !connection.hasAccess) return;
        long now = SystemClock.elapsedRealtime();
        if (!manual && lastCheck >= 0 && now - lastCheck < 15 * 60 * 1000L) return;
        lastCheck = now; checking = true; endpointError = "";
        long version = generation;
        changed();
        // Read-only and independent: an unreachable server cannot delay offline restoration.
        checks.execute(() -> {
            int count = 0; String failure = "";
            try { count = controller.endpointPlan().getJSONArray("changes").length(); }
            catch (Exception e) { failure = error(e); }
            int result = count; String problem = failure;
            main.post(() -> {
                checking = false;
                if (version == generation && !busy() && connection != null && connection.screen == ConnectionState.Screen.CONNECTED) {
                    endpointChanges = result; endpointError = problem;
                    if (manual) message = problem.isEmpty() ? text(result == 0 ? R.string.endpoints_current : R.string.update_available) : problem;
                }
                if (version != generation) lastCheck = -1;
                changed();
            });
        });
    }
    void updateEndpoints() {
        run(R.string.progress_updating, () -> { message = controller.applyEndpointUpdate(); endpointChanges = 0; }, false);
    }
    void recoveryStatus() {
        run(R.string.progress_checking_server, () -> { message = controller.recoveryStatus().optString("summary", text(R.string.recovery_status_default)); }, false);
    }
    void listAudio() {
        run(R.string.progress_checking_server, () -> {
            JSONArray items = controller.audioRecoveries();
            if (items.length() == 0) message = text(R.string.audio_none);
            else prompt = new Prompt("audio-list", items);
        }, false);
    }
    void inspectAudio(String id) {
        run(R.string.progress_checking, () -> {
            AudioCacheRepair repair = controller.audioPacket(id);
            String result = controller.inspectAudio(repair);
            if ("repairable".equals(result)) prompt = new Prompt("audio-repair", repair);
            else message = text("healthy".equals(result) ? R.string.audio_healthy : "missing".equals(result) ? R.string.audio_missing : R.string.audio_mismatch);
        }, false);
    }
    void repairAudio(AudioCacheRepair repair) {
        run(R.string.status_working, () -> { controller.repairAudio(repair); message = text(R.string.audio_recovered); }, false);
    }
    private String error(Exception e) {
        Throwable cause = e;
        for (int depth = 0; cause != null && depth < 32; depth++, cause = cause.getCause()) {
            if (cause instanceof UserError) return ((UserError)cause).text(context);
        }
        return text(R.string.operation_failed);
    }
}
