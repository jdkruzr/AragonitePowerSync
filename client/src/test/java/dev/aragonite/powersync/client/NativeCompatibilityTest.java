package dev.aragonite.powersync.client;

import org.junit.Test;
import static org.junit.Assert.*;

public class NativeCompatibilityTest {
    @Test public void go6ReportsActualFirmwareWithoutQualificationGate() {
        assertEquals("model=Go6_2; API=33; firmware=4.2.2-rel_0924; ksync=32198; attempting native interfaces",
            NativeCompatibility.describe("Go6_2", 33, "4.2.2-rel_0924", 32198));
    }

    @Test public void unknownDevicesAndFirmwareUpdatesRemainObservations() {
        for (String model : new String[]{null, "", "FutureBOOX", "TabUltraCPro", "Palma2_Pro_C", "Go103_2Lumi"}) {
            for (int sdk : new int[]{33, 34, 35, 36}) {
                for (long ksync : new long[]{32173, 32174, 32198, 99999}) {
                    String report = NativeCompatibility.describe(model, sdk, null, ksync);
                    assertTrue(report.contains("API=" + sdk));
                    assertTrue(report.contains("ksync=" + ksync));
                    assertTrue(report.endsWith("attempting native interfaces"));
                }
            }
        }
    }

    @Test public void diagnosticStaysOnOneLineWithMissingOrMalformedBuildFields() {
        String report = NativeCompatibility.describe(null, 33, "build\r\nnext", 32198);
        assertTrue(report.contains("model=unknown"));
        assertFalse(report.contains("\r"));
        assertFalse(report.contains("\n"));
    }
}
