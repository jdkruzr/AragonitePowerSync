package dev.aragonite.powersync.client;

import org.json.JSONObject;
import java.util.Set;

/** Offer continuation only after an owned server switch, never during rollback. */
final class ConnectionRecovery {
    private static final Set<String> PHASES = Set.of("failed", "switch_requested", "switched", "catalog_restore_requested",
        "login_requested", "authenticated", "enable_requested");

    static boolean canResume(JSONObject status) {
        return PHASES.contains(status.optString("phase"))
            && status.optBoolean("hasBaseline") && status.optBoolean("available")
            && status.optBoolean("allFilesAccess") && !status.optBoolean("endpointInterrupted")
            && status.optBoolean("configurationMatches") && status.optBoolean("enrolledClusterSelected")
            && (!status.optBoolean("accountLoggedIn") || status.optBoolean("identityMatches"));
    }
}
