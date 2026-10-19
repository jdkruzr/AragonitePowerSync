package dev.aragonite.powersync.client;

/** Firmware observations for diagnostics, never an admission policy. */
final class NativeCompatibility {
    private NativeCompatibility() {}

    static String describe(String model, int sdk, String firmware, long ksync) {
        return "model=" + field(model) + "; API=" + sdk + "; firmware=" + field(firmware)
            + "; ksync=" + ksync + "; attempting native interfaces";
    }

    private static String field(String value) {
        return value == null || value.isEmpty() ? "unknown" : value.replace('\n', ' ').replace('\r', ' ');
    }
}
