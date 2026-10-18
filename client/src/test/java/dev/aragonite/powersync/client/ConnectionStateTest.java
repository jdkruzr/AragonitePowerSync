package dev.aragonite.powersync.client;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConnectionStateTest {
    private JSONObject active() throws Exception {
        return new JSONObject().put("available", true).put("phase", "active").put("hasBaseline", true)
            .put("identityMatches", true).put("configurationMatches", true).put("allFilesAccess", true)
            .put("flags", new JSONObject().put("notes", "true").put("reader", "true"));
    }
    @Test public void existingInstallationOpensConnectedWithoutMigration() throws Exception {
        ConnectionState state = new ConnectionState(active());
        assertEquals(ConnectionState.Screen.CONNECTED, state.screen);
        assertFalse(state.canEnable()); assertTrue(state.canRestore);
    }
    @Test public void journalAloneDoesNotProveConnection() throws Exception {
        for (String key : new String[]{"available", "identityMatches", "configurationMatches"}) {
            ConnectionState state = new ConnectionState(active().put(key, false));
            assertEquals(key, ConnectionState.Screen.ATTENTION, state.screen);
            assertFalse(state.canEnable()); assertTrue(state.canRestore);
        }
    }
    @Test public void interruptionRequiresRecoveryEvenWithMatchingLiveAccount() throws Exception {
        for (String phase : new String[]{"prepared", "login_requested", "authenticated", "enable_requested", "failed", "restore_requested", "needs_signin"}) {
            ConnectionState state = new ConnectionState(active().put("phase", phase));
            assertEquals(phase, ConnectionState.Screen.ATTENTION, state.screen);
            assertTrue(state.canRestore); assertFalse(state.canEnable());
        }
        assertEquals(ConnectionState.Screen.ATTENTION, new ConnectionState(active().put("endpointInterrupted", true)).screen);
    }
    @Test public void pausedFlagsAreReadOnlyAndOfferExplicitEnable() throws Exception {
        JSONObject snapshot = active().put("flags", new JSONObject().put("notes", "false").put("reader", "true"));
        String before = snapshot.toString();
        ConnectionState state = new ConnectionState(snapshot);
        assertEquals(ConnectionState.Screen.CONNECTED, state.screen);
        assertTrue(state.canEnable()); assertFalse(state.notesEnabled); assertTrue(state.readerEnabled);
        assertEquals(before, snapshot.toString());
        assertFalse(new ConnectionState(snapshot.put("allFilesAccess", false)).canEnable());
    }
    @Test public void restoredProfileDoesNotOfferReusingOldEnrollment() throws Exception {
        JSONObject snapshot = active().put("phase", "restored").put("enrollmentReady", false);
        ConnectionState state = new ConnectionState(snapshot);
        assertEquals(ConnectionState.Screen.SETUP, state.screen); assertFalse(state.canRestore);
        assertEquals(ConnectionState.Screen.ENROLLED, new ConnectionState(snapshot.put("enrollmentReady", true)).screen);
    }
    @Test public void initialEnrollmentCanContinueWithNoBaseline() throws Exception {
        JSONObject snapshot = new JSONObject().put("available", true).put("phase", "not_configured");
        assertEquals(ConnectionState.Screen.SETUP, new ConnectionState(snapshot).screen);
        assertEquals(ConnectionState.Screen.ENROLLED, new ConnectionState(snapshot.put("enrollmentReady", true)).screen);
    }
    @Test public void unknownFlagsNeverAppearEnabled() throws Exception {
        ConnectionState state = new ConnectionState(active().put("flags", new JSONObject()));
        assertFalse(state.notesEnabled); assertFalse(state.readerEnabled);
    }
}
