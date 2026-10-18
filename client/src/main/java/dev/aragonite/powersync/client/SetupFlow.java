package dev.aragonite.powersync.client;

import org.json.JSONObject;

/** Durable enrollment sequencing, independent of an Activity or Android lifecycle. */
final class SetupFlow {
    interface Store {
        JSONObject pending() throws Exception;
        void save(JSONObject pending) throws Exception;
        void clear() throws Exception;
    }
    interface Connection {
        boolean enrolled() throws Exception;
        void enroll(String server, String code) throws Exception;
        void connect() throws Exception;
    }
    static final class FreshCodeRequired extends Exception {}
    private final Store store;
    private final Connection connection;
    SetupFlow(Store store, Connection connection) { this.store = store; this.connection = connection; }

    void confirm() throws Exception {
        if (!connection.enrolled()) {
            JSONObject pending = store.pending();
            if (pending == null || !"draft".equals(pending.optString("stage"))) throw new FreshCodeRequired();
            String server = pending.getString("server"), code = pending.getString("code");
            // A consumed or ambiguously consumed code must never be retried after process death.
            store.save(new JSONObject().put("stage", "redeeming").put("server", server));
            connection.enroll(server, code);
        }
        store.clear();
        connection.connect();
    }
}
