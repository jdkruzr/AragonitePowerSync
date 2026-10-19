package dev.aragonite.powersync.client;

/** Pin the vendor interface separately from the app's minimum Android version. */
final class NativeCompatibility {
    private NativeCompatibility() {}

    static long expectedKsync(String model, int sdk) {
        if (sdk == 35 && "Palma2_Pro_C".equals(model)) return 32173;
        if (sdk == 35 && "Go103_2Lumi".equals(model)) return 32079;
        // Android 13 TUCP: interface comparison and ordinary-UID read checks.
        // End-to-end sync qualification is recorded separately in docs.
        if (sdk == 33 && "TabUltraCPro".equals(model)) return 32174;
        return -1;
    }
}
