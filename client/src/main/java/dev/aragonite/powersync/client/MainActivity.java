package dev.aragonite.powersync.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

/** State-driven companion UI. Native operations live in the process-owned coordinator. */
public final class MainActivity extends Activity implements PowerSyncCoordinator.Observer {
    private PowerSyncCoordinator coordinator;
    private LinearLayout content;
    private TextView operation, connectionStatus;
    private String page = "home", rendered = "";
    private boolean awaitingPermission, foreground;
    private AlertDialog dialog;
    private PowerSyncCoordinator.Prompt shownPrompt;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        coordinator = PowerSyncCoordinator.get(this);
        if (saved != null) {
            page = saved.getString("page", "home");
            awaitingPermission = saved.getBoolean("permission");
        }
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20); root.setPadding(pad, pad, pad, pad);
        scroll.addView(root); setContentView(scroll);
        TextView title = new TextView(this); title.setText(R.string.app_name); title.setTextSize(30); root.addView(title);
        connectionStatus = new TextView(this); connectionStatus.setTextSize(19); connectionStatus.setPadding(0, pad, 0, pad); root.addView(connectionStatus);
        operation = new TextView(this); operation.setTextSize(16); root.addView(operation);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); root.addView(content);
    }
    @Override protected void onResume() {
        super.onResume(); foreground = true;
        coordinator.localeChanged(getResources().getConfiguration().getLocales().toLanguageTags());
        // Refresh before enabling controls or delivering a retained confirmation.
        if (awaitingPermission) { awaitingPermission = false; coordinator.permissionReturned(); }
        else coordinator.refresh();
        coordinator.observe(this);
    }
    @Override protected void onStop() {
        foreground = false; coordinator.detach(this);
        if (dialog != null) {
            if (shownPrompt != null) coordinator.retainPrompt(shownPrompt);
            dialog.setOnCancelListener(null); dialog.dismiss(); dialog = null; shownPrompt = null;
        }
        super.onStop();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("page", page); out.putBoolean("permission", awaitingPermission);
        super.onSaveInstanceState(out);
    }
    @Override public void onBackPressed() {
        if (!"home".equals(page)) { page = "home"; rendered = ""; changed(); }
        else super.onBackPressed();
    }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density); }
    private void navigate(String target) { page = target; rendered = ""; changed(); }
    private void text(String value) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(17); view.setPadding(0, dp(8), 0, dp(8)); content.addView(view);
    }
    private void text(int resource) { text(getString(resource)); }
    private void button(int resource, Runnable action) {
        Button button = new Button(this); button.setText(resource); button.setAllCaps(false);
        button.setEnabled(!coordinator.busy()); button.setOnClickListener(v -> { if (!coordinator.busy()) action.run(); }); content.addView(button);
    }
    @Override public void changed() {
        if (!foreground) return;
        ConnectionState state = coordinator.connection;
        operation.setText(coordinator.busy() ? getString(coordinator.progressResource == 0 ? R.string.status_working : coordinator.progressResource) : coordinator.message);
        operation.setVisibility(operation.length() == 0 ? View.GONE : View.VISIBLE);
        if (state == null) { connectionStatus.setText(R.string.progress_checking); return; }
        int title = switch(state.screen) {
            case SETUP -> R.string.setup_heading;
            case ENROLLED -> R.string.enrolled_heading;
            case CONNECTED -> R.string.connected_heading;
            case ATTENTION -> R.string.attention_heading;
        };
        connectionStatus.setText(state.screen == ConnectionState.Screen.SETUP || state.server.isEmpty() ? getString(title) : getString(R.string.heading_server, getString(title), state.server));
        boolean editing = "home".equals(page) && state.screen == ConnectionState.Screen.SETUP && !coordinator.hasDraft;
        if (editing) getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        String key = page + state.screen + state.snapshot.toString() + coordinator.busy() + coordinator.hasDraft + coordinator.needsFreshCode + coordinator.checking + coordinator.endpointChanges + coordinator.endpointError + coordinator.statusError;
        if (!key.equals(rendered)) {
            rendered = key; content.removeAllViews();
            if (!"home".equals(page)) button(R.string.back_home, () -> navigate("home"));
            switch(page) {
                case "manage" -> manage(state);
                case "troubleshoot" -> troubleshoot(state);
                case "details" -> details(state);
                default -> home(state);
            }
        }
        if (!coordinator.busy() && dialog == null) {
            PowerSyncCoordinator.Prompt prompt = coordinator.takePrompt();
            if (prompt != null) showPrompt(prompt);
        }
    }
    private void home(ConnectionState state) {
        switch(state.screen) {
            case SETUP -> {
                text(R.string.setup_explanation);
                if (coordinator.hasDraft) {
                    text(coordinator.pendingServer);
                    button(R.string.continue_setup, coordinator::continueSetup);
                    button(R.string.change_details, coordinator::changeSetup);
                } else {
                    if (coordinator.needsFreshCode) text(R.string.error_fresh_code);
                    text(R.string.server_label);
                    EditText server = input(R.string.hint_server, false, coordinator.draftServer.isEmpty() ? coordinator.pendingServer : coordinator.draftServer);
                    text(R.string.code_label);
                    EditText code = input(R.string.hint_code, true, coordinator.draftCode);
                    server.addTextChangedListener(watch(value -> coordinator.draftServer = value));
                    code.addTextChangedListener(watch(value -> coordinator.draftCode = value));
                    button(R.string.connect_alexandria, () -> {
                        String target = server.getText().toString(), secret = code.getText().toString();
                        coordinator.draftCode = ""; code.setText("");
                        coordinator.prepare(target, secret);
                    });
                }
            }
            case ENROLLED -> {
                text(R.string.enrolled_explanation);
                button(R.string.continue_setup, coordinator::continueSetup);
                button(R.string.change_details, coordinator::changeSetup);
            }
            case CONNECTED -> {
                text(getString(R.string.sync_summary, flag(state, "notes"), flag(state, "reader")));
                text(R.string.sync_completion_note);
                if (state.canEnable()) button(R.string.enable_sync, coordinator::enableSync);
                if (!state.hasAccess) button(R.string.grant_access, this::requestAccess);
                button(R.string.open_alexandria, () -> openServer(state.server));
                if (coordinator.endpointChanges > 0) button(R.string.update_available, () -> showPrompt(new PowerSyncCoordinator.Prompt("endpoints", null)));
                button(R.string.manage_connection, () -> navigate("manage"));
            }
            case ATTENTION -> {
                text(state.snapshot.optBoolean("endpointInterrupted") ? R.string.endpoint_interrupted : R.string.attention_explanation);
                if (!coordinator.statusError.isEmpty()) text(coordinator.statusError);
                if (!state.hasAccess) button(R.string.grant_access, this::requestAccess);
                if (state.canResume) {
                    text(R.string.resume_connection_explanation);
                    button(R.string.resume_connection, coordinator::resumeConnection);
                }
                if (state.canRestore) button(R.string.button_return, () -> showPrompt(new PowerSyncCoordinator.Prompt("restore", null)));
                button(R.string.button_refresh_status, coordinator::refresh);
            }
        }
        button(R.string.troubleshooting, () -> navigate("troubleshoot"));
    }
    private void manage(ConnectionState state) {
        text(R.string.manage_explanation);
        if (!state.hasAccess) button(R.string.grant_access, this::requestAccess);
        if (state.canRestore) button(R.string.button_return, () -> showPrompt(new PowerSyncCoordinator.Prompt("restore", null)));
        button(R.string.technical_details, () -> navigate("details"));
    }
    private void troubleshoot(ConnectionState state) {
        if (!state.hasAccess) button(R.string.grant_access, this::requestAccess);
        button(R.string.button_refresh_status, coordinator::refresh);
        if (state.screen == ConnectionState.Screen.CONNECTED) {
            if (state.hasAccess) {
                button(R.string.check_updates, () -> coordinator.checkEndpoints(true));
                if (coordinator.checking) text(R.string.progress_checking_server);
                if (!coordinator.endpointError.isEmpty()) text(coordinator.endpointError);
                if (coordinator.endpointChanges > 0) button(R.string.update_available, () -> showPrompt(new PowerSyncCoordinator.Prompt("endpoints", null)));
                button(R.string.button_recover_audio, coordinator::listAudio);
            }
            button(R.string.button_recovery_status, coordinator::recoveryStatus);
        }
        button(R.string.button_open_settings, () -> {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.onyx");
            startActivity(intent == null ? new Intent(Settings.ACTION_SETTINGS) : intent);
        });
        button(R.string.technical_details, () -> navigate("details"));
        if (state.canRestore) button(R.string.button_return, () -> showPrompt(new PowerSyncCoordinator.Prompt("restore", null)));
    }
    private String flag(ConnectionState state, String key) {
        JSONObject flags = state.snapshot.optJSONObject("flags");
        String value = flags == null ? "" : flags.optString(key);
        return getString("true".equals(value) ? R.string.sync_on : "false".equals(value) ? R.string.sync_paused : R.string.state_unknown);
    }
    private void details(ConnectionState state) {
        JSONObject s = state.snapshot;
        text(getString(R.string.status_description, s.optString("clusterId", getString(R.string.state_unknown)),
            getString(s.optBoolean("accountLoggedIn") ? R.string.account_signed_in : R.string.account_signed_out),
            flag(state, "notes"), flag(state, "reader"), s.optString("phase"), getString(s.optBoolean("hasBaseline") ? R.string.yes : R.string.no)));
        if (!coordinator.statusError.isEmpty()) text(coordinator.statusError);
        if (!coordinator.endpointError.isEmpty()) text(coordinator.endpointError);
    }
    private EditText input(int hint, boolean secret, String value) {
        EditText edit = new EditText(this); edit.setHint(hint); edit.setSingleLine(true); edit.setSaveEnabled(false);
        edit.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        edit.setInputType(android.text.InputType.TYPE_CLASS_TEXT | (secret ? android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD : android.text.InputType.TYPE_TEXT_VARIATION_URI));
        edit.setText(value); edit.setEnabled(!coordinator.busy()); content.addView(edit); return edit;
    }
    private interface Changed { void update(String value); }
    private TextWatcher watch(Changed changed) {
        return new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { changed.update(s.toString()); }
            public void afterTextChanged(Editable e) {}
        };
    }
    private void requestAccess() {
        awaitingPermission = true;
        startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getPackageName())));
    }
    private void openServer(String server) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SafeURL.base(server)))); }
        catch (Exception e) { coordinator.showMessage(getString(R.string.open_browser_failed)); }
    }
    private void showPrompt(PowerSyncCoordinator.Prompt prompt) {
        if ("permission".equals(prompt.kind)) { requestAccess(); return; }
        shownPrompt = prompt;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch(prompt.kind) {
            case "connect" -> builder.setTitle(R.string.dialog_connect_title)
                .setMessage(getString(R.string.confirm_connection, prompt.data))
                .setPositiveButton(R.string.action_connect, (d, w) -> { shownPrompt = null; coordinator.connect(); });
            case "restore" -> builder.setTitle(R.string.dialog_return_title).setMessage(R.string.dialog_return_message)
                .setPositiveButton(R.string.action_restore, (d, w) -> { shownPrompt = null; coordinator.restore(); });
            case "endpoints" -> builder.setTitle(R.string.update_available).setMessage(R.string.update_confirmation)
                .setPositiveButton(R.string.action_update, (d, w) -> { shownPrompt = null; coordinator.updateEndpoints(); });
            case "audio-list" -> {
                JSONArray items = (JSONArray)prompt.data;
                String[] labels = new String[items.length()];
                for (int i = 0; i < labels.length; i++) {
                    JSONObject item = items.optJSONObject(i);
                    labels[i] = getString(R.string.audio_label, item.optString("title"), item.optString("fileName"));
                }
                builder.setTitle(R.string.dialog_audio_choose_title).setItems(labels, (d, n) -> {
                    shownPrompt = null; coordinator.inspectAudio(items.optJSONObject(n).optString("operationId"));
                });
            }
            case "audio-repair" -> builder.setTitle(R.string.dialog_audio_recover_title).setMessage(R.string.dialog_audio_recover_message)
                .setPositiveButton(R.string.action_recover, (d, w) -> { shownPrompt = null; coordinator.repairAudio((AudioCacheRepair)prompt.data); });
            default -> { shownPrompt = null; return; }
        }
        Runnable cancel = () -> { shownPrompt = null; if ("connect".equals(prompt.kind)) coordinator.cancelSetup(); };
        builder.setNegativeButton(R.string.action_cancel, (d, w) -> cancel.run()).setOnCancelListener(d -> cancel.run());
        dialog = builder.create();
        dialog.setOnDismissListener(d -> { dialog = null; shownPrompt = null; });
        dialog.show();
    }
}
