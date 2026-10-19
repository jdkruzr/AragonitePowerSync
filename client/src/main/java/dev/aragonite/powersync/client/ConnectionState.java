package dev.aragonite.powersync.client;

import org.json.JSONObject;

/** A read-only presentation policy. Never infer live connection health from a journal alone. */
final class ConnectionState {
    enum Screen { SETUP, ENROLLED, CONNECTED, ATTENTION }
    final JSONObject snapshot;
    final Screen screen;
    final boolean canRestore, canResume, hasAccess, notesEnabled, readerEnabled;
    final String server;

    ConnectionState(JSONObject s) {
        snapshot = s;
        server = s.optString("serverURL");
        String phase = s.optString("phase", "not_configured");
        canRestore = s.optBoolean("hasBaseline") && !"restored".equals(phase);
        canResume = ConnectionRecovery.canResume(s);
        hasAccess = s.optBoolean("allFilesAccess");
        JSONObject flags = s.optJSONObject("flags");
        notesEnabled = flags != null && "true".equals(flags.optString("notes"));
        readerEnabled = flags != null && "true".equals(flags.optString("reader"));
        boolean settled = "not_configured".equals(phase) || "restored".equals(phase);
        if (!s.optBoolean("available") || s.optBoolean("endpointInterrupted")) screen = Screen.ATTENTION;
        else if ("active".equals(phase) && s.optBoolean("identityMatches") && s.optBoolean("configurationMatches")) screen = Screen.CONNECTED;
        else if (!settled) screen = Screen.ATTENTION;
        else screen = s.optBoolean("enrollmentReady") ? Screen.ENROLLED : Screen.SETUP;
    }

    boolean canEnable() { return screen == Screen.CONNECTED && hasAccess && (!notesEnabled || !readerEnabled); }
}
