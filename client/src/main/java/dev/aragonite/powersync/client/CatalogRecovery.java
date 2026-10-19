package dev.aragonite.powersync.client;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.LinkedHashMap;
import java.util.Map;

/** Restore catalog membership independently of whether book bytes are downloaded. */
final class CatalogRecovery {
    interface Store {
        JSONArray read() throws Exception;
        void insert(JSONObject row) throws Exception;
    }
    interface Files { boolean exists(String path); }

    private static Map<String, String> index(JSONArray rows) throws Exception {
        Map<String, String> result = new LinkedHashMap<>();
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String uuid = row.getString("uuid");
            long id = row.getLong("id");
            if (uuid.isEmpty() || id <= 0 || !ids.add(id) || result.put(uuid, signature(row)) != null)
                throw new UserError(R.string.error_catalog_identity_conflict);
        }
        return result;
    }

    private static String signature(JSONObject row) throws Exception {
        Map<String, String> values = new java.util.TreeMap<>();
        for (java.util.Iterator<String> keys = row.keys(); keys.hasNext();) {
            String key = keys.next();
            Object value = row.get(key);
            if (value != JSONObject.NULL && !(value instanceof String) && !(value instanceof Number))
                throw new UserError(R.string.error_unsupported_catalog_field);
            values.put(key, new JSONArray().put(value).toString());
        }
        return values.toString();
    }

    static int restore(JSONArray saved, Store store) throws Exception {
        index(saved); // Validate every saved row before writing anything.
        JSONArray initial = store.read();
        Map<String, String> expected = index(initial);
        java.util.Set<Long> occupied = new java.util.HashSet<>();
        for (int i = 0; i < initial.length(); i++) occupied.add(initial.getJSONObject(i).getLong("id"));
        for (int i = 0; i < saved.length(); i++) {
            JSONObject row = saved.getJSONObject(i);
            if (!expected.containsKey(row.getString("uuid")) && occupied.contains(row.getLong("id")))
                throw new UserError(R.string.error_original_catalog_id_unavailable_for_repair);
        }
        int restored = 0;
        for (int i = 0; i < saved.length(); i++) {
            JSONObject row = saved.getJSONObject(i);
            String uuid = row.getString("uuid");
            if (expected.containsKey(uuid)) continue; // Never overwrite an existing book.
            unchanged(store, expected);
            store.insert(new JSONObject(row.toString()));
            expected.put(uuid, signature(row));
            unchanged(store, expected);
            restored++;
        }
        unchanged(store, expected);
        return restored;
    }

    private static void unchanged(Store store, Map<String, String> expected) throws Exception {
        if (!index(store.read()).equals(expected))
            throw new UserError(R.string.error_restored_catalog_readback_differs);
    }

    static JSONObject fileAvailability(JSONArray catalog, Files files) throws Exception {
        JSONObject result = new JSONObject();
        for (int i = 0; i < catalog.length(); i++) {
            JSONObject row = catalog.getJSONObject(i);
            result.put(row.getString("uuid"), files.exists(row.optString("nativeAbsolutePath"))
                || files.exists(row.optString("nocasePath")));
        }
        return result;
    }

    static int newlyUnavailable(JSONObject before, JSONObject now) {
        if (before == null) return 0; // Older baselines did not record local availability.
        int count = 0;
        for (java.util.Iterator<String> keys = before.keys(); keys.hasNext();) {
            String uuid = keys.next();
            if (before.optBoolean(uuid) && now.has(uuid) && !now.optBoolean(uuid)) count++;
        }
        return count;
    }
}
