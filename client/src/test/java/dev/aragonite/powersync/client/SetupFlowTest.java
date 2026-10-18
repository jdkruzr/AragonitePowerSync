package dev.aragonite.powersync.client;

import org.json.JSONObject;
import org.junit.Test;
import java.io.IOException;
import static org.junit.Assert.*;

public class SetupFlowTest {
    private static final class Fixture implements SetupFlow.Store, SetupFlow.Connection {
        JSONObject pending;
        boolean enrolled, loseResponse, failConnect;
        int redemptions, connections;
        Fixture() throws Exception { pending = new JSONObject().put("stage", "draft").put("server", "https://example.test").put("code", "synthetic"); }
        public JSONObject pending() { return pending; }
        public void save(JSONObject value) { pending = value; }
        public void clear() { pending = null; }
        public boolean enrolled() { return enrolled; }
        public void enroll(String server, String code) throws Exception {
            assertEquals("https://example.test", server); assertEquals("synthetic", code);
            assertEquals("redeeming", pending.getString("stage")); assertFalse(pending.has("code"));
            redemptions++;
            if (loseResponse) throw new IOException("synthetic lost response");
            enrolled = true;
        }
        public void connect() throws Exception {
            connections++; assertTrue(enrolled); assertNull(pending);
            if (failConnect) throw new IOException("synthetic preflight failure");
        }
        SetupFlow flow() { return new SetupFlow(this, this); }
    }
    @Test public void confirmationEnrollsSavesThenConnects() throws Exception {
        Fixture f = new Fixture(); f.flow().confirm();
        assertEquals(1, f.redemptions); assertEquals(1, f.connections); assertNull(f.pending);
    }
    @Test public void recreationAfterEnrollmentDoesNotSpendCodeAgain() throws Exception {
        Fixture f = new Fixture(); f.enrolled = true;
        f.pending = new JSONObject().put("stage", "redeeming").put("server", "https://example.test");
        f.flow().confirm(); assertEquals(0, f.redemptions); assertEquals(1, f.connections);
    }
    @Test public void lostResponseRequiresFreshCodeEvenAfterRestart() throws Exception {
        Fixture f = new Fixture(); f.loseResponse = true;
        try { f.flow().confirm(); fail(); } catch (IOException expected) {}
        try { f.flow().confirm(); fail(); } catch (SetupFlow.FreshCodeRequired expected) {}
        assertEquals(1, f.redemptions); assertEquals(0, f.connections); assertFalse(f.pending.has("code"));
    }
    @Test public void savedEnrollmentSurvivesPreflightFailure() throws Exception {
        Fixture f = new Fixture(); f.failConnect = true;
        try { f.flow().confirm(); fail(); } catch (IOException expected) {}
        f.failConnect = false; f.flow().confirm();
        assertEquals(1, f.redemptions); assertEquals(2, f.connections);
    }
    @Test public void cancelOrMissingDraftCannotRedeem() throws Exception {
        Fixture f = new Fixture(); f.clear();
        try { f.flow().confirm(); fail(); } catch (SetupFlow.FreshCodeRequired expected) {}
        assertEquals(0, f.redemptions); assertEquals(0, f.connections);
    }
}
