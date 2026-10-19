package dev.aragonite.powersync.client;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConnectionRecoveryTest {
    private JSONObject interrupted() throws Exception {
        return new JSONObject().put("phase", "failed").put("hasBaseline", true).put("available", true)
            .put("allFilesAccess", true).put("configurationMatches", true).put("enrolledClusterSelected", true);
    }
    @Test public void oldFailedTransitionCanResumeBeforeSignIn() throws Exception {
        JSONObject state = interrupted().put("accountLoggedIn", false);
        assertTrue(ConnectionRecovery.canResume(state));
        assertTrue(new ConnectionState(state).canResume);
        assertEquals(ConnectionState.Screen.ATTENTION, new ConnectionState(state).screen);
    }
    @Test public void matchingSignInCanContinueWithoutAnotherLogin() throws Exception {
        for (String phase : new String[]{"switch_requested", "switched", "catalog_restore_requested", "login_requested", "authenticated", "enable_requested", "failed"})
            assertTrue(ConnectionRecovery.canResume(interrupted().put("phase", phase).put("accountLoggedIn", true).put("identityMatches", true)));
    }
    @Test public void foreignAccountConfigurationOrServerCannotResume() throws Exception {
        assertFalse(ConnectionRecovery.canResume(interrupted().put("accountLoggedIn", true).put("identityMatches", false)));
        for (String key : new String[]{"hasBaseline", "available", "allFilesAccess", "configurationMatches", "enrolledClusterSelected"})
            assertFalse(key, ConnectionRecovery.canResume(interrupted().put(key, false)));
        assertFalse(ConnectionRecovery.canResume(interrupted().put("endpointInterrupted", true)));
    }
    @Test public void rollbackAndSettledStatesNeverResumeEnrollment() throws Exception {
        for (String phase : new String[]{"not_configured", "prepared", "configure_requested", "restore_requested", "restored", "needs_signin", "active", "unknown"})
            assertFalse(phase, ConnectionRecovery.canResume(interrupted().put("phase", phase)));
    }
}
