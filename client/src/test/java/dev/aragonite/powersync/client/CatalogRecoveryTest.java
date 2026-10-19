package dev.aragonite.powersync.client;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.io.IOException;
import static org.junit.Assert.*;

public class CatalogRecoveryTest {
    private static JSONObject book(int id) throws Exception {
        return new JSONObject().put("id", id).put("uuid", "book-" + id)
            .put("nativeAbsolutePath", "/not-downloaded/book-" + id + ".epub")
            .put("nocasePath", "/not-downloaded/book-" + id + ".epub")
            .put("cloudId", "cloud-" + id).put("fileSyncStatus", 2)
            .put("progress", "12/100").put("extraInfo", JSONObject.NULL);
    }
    private static JSONArray books(int... ids) throws Exception {
        JSONArray rows = new JSONArray();
        for (int id : ids) rows.put(book(id));
        return rows;
    }
    private static class Memory implements CatalogRecovery.Store {
        JSONArray data;
        int writes, failAfter = -1;
        boolean corruptInsert, changeBeforeWrite;
        int reads;
        Memory(JSONArray rows) throws Exception { data = new JSONArray(rows.toString()); }
        public JSONArray read() throws Exception {
            if (++reads == 2 && changeBeforeWrite) data.getJSONObject(0).put("progress", "90/100");
            return new JSONArray(data.toString());
        }
        public void insert(JSONObject row) throws Exception {
            if (corruptInsert) row.put("cloudId", "wrong-cloud");
            data.put(row);
            if (++writes == failAfter) throw new IOException("simulated process loss after insert");
        }
    }
    @Test public void restoresUndownloadedBooksWithAllMetadataAndNoDuplicateOnRetry() throws Exception {
        JSONArray saved = books(1, 2, 3);
        Memory store = new Memory(books(3));
        assertEquals(2, CatalogRecovery.restore(saved, store));
        assertEquals(3, store.data.length());
        for (int i = 0; i < store.data.length(); i++) {
            JSONObject row = store.data.getJSONObject(i);
            assertEquals("cloud-" + row.getInt("id"), row.getString("cloudId"));
            assertEquals(2, row.getInt("fileSyncStatus"));
            assertEquals("12/100", row.getString("progress"));
            assertTrue(row.isNull("extraInfo"));
        }
        assertEquals(0, CatalogRecovery.restore(saved, store));
        assertEquals(2, store.writes);
    }
    @Test public void preservesExistingEditsAndUnrelatedRows() throws Exception {
        JSONArray current = books(2, 99);
        current.getJSONObject(0).put("progress", "80/100");
        Memory store = new Memory(current);
        assertEquals(1, CatalogRecovery.restore(books(1, 2), store));
        assertEquals("80/100", store.data.getJSONObject(0).getString("progress"));
        assertEquals("book-99", store.data.getJSONObject(1).getString("uuid"));
    }
    @Test public void detectsLateIdCollisionBeforeAnyInsertion() throws Exception {
        Memory store = new Memory(new JSONArray().put(book(2).put("uuid", "unrelated")));
        try { CatalogRecovery.restore(books(1, 2), store); fail(); } catch (UserError expected) {}
        assertEquals(0, store.writes);
    }
    @Test public void rejectsDuplicateSavedIdentitiesBeforeWriting() throws Exception {
        for (JSONArray invalid : new JSONArray[]{books(1, 1), new JSONArray().put(book(1)).put(book(2).put("uuid", "book-1"))}) {
            Memory store = new Memory(new JSONArray());
            try { CatalogRecovery.restore(invalid, store); fail(); } catch (UserError expected) {}
            assertEquals(0, store.writes);
        }
    }
    @Test public void resumesAfterEveryCompletedInsert() throws Exception {
        for (int stop = 1; stop <= 3; stop++) {
            Memory store = new Memory(new JSONArray()); store.failAfter = stop;
            try { CatalogRecovery.restore(books(1, 2, 3), store); fail(); } catch (IOException expected) {}
            store.failAfter = -1;
            CatalogRecovery.restore(books(1, 2, 3), store);
            assertEquals(3, store.data.length()); assertEquals(3, store.writes);
        }
    }
    @Test public void readbackDetectsProviderChanges() throws Exception {
        Memory store = new Memory(new JSONArray()); store.corruptInsert = true;
        try { CatalogRecovery.restore(books(1, 2), store); fail(); } catch (UserError expected) {}
        assertEquals(1, store.writes);
    }
    @Test public void concurrentCatalogChangeStopsBeforeWriting() throws Exception {
        Memory store = new Memory(books(9)); store.changeBeforeWrite = true;
        try { CatalogRecovery.restore(books(1), store); fail(); } catch (UserError expected) {}
        assertEquals(0, store.writes);
    }
    @Test public void tracksLocalAvailabilitySeparatelyAndAcceptsOldBaselines() throws Exception {
        JSONArray catalog = books(1, 2);
        catalog.getJSONObject(1).put("nocasePath", "/downloaded.epub");
        JSONObject before = CatalogRecovery.fileAvailability(catalog, path -> path.equals("/downloaded.epub"));
        assertFalse(before.getBoolean("book-1")); assertTrue(before.getBoolean("book-2"));
        JSONObject after = CatalogRecovery.fileAvailability(catalog, path -> false);
        assertEquals(1, CatalogRecovery.newlyUnavailable(before, after));
        assertEquals(0, CatalogRecovery.newlyUnavailable(null, after));
        assertEquals(0, CatalogRecovery.newlyUnavailable(after, after));
    }
}
