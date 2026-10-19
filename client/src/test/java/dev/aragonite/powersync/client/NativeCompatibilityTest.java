package dev.aragonite.powersync.client;

import org.junit.Test;
import static org.junit.Assert.*;

public class NativeCompatibilityTest {
    @Test public void knownFirmwarePairsRemainPinned() {
        assertEquals(32173, NativeCompatibility.expectedKsync("Palma2_Pro_C", 35));
        assertEquals(32079, NativeCompatibility.expectedKsync("Go103_2Lumi", 35));
        assertEquals(32174, NativeCompatibility.expectedKsync("TabUltraCPro", 33));
    }

    @Test public void unknownModelsAndAndroidVersionsStayBlocked() {
        for (String model : new String[]{null, "", "TabUltraC", "TabUltraCPro", "Palma2_Pro_C", "Go103_2Lumi"}) {
            for (int sdk : new int[]{24, 32, 33, 34, 35, 36}) {
                boolean known = sdk == 33 && "TabUltraCPro".equals(model)
                    || sdk == 35 && ("Palma2_Pro_C".equals(model) || "Go103_2Lumi".equals(model));
                if (!known) assertEquals(-1, NativeCompatibility.expectedKsync(model, sdk));
            }
        }
    }
}
