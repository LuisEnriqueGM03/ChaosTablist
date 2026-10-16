package com.chaos.tablist.text;

import java.util.Map;
import com.chaos.tablist.server.ServerValues;

/** The preview and the server use exactly the same vanilla placeholder fallbacks. */
public final class VanillaValues {
    private VanillaValues() {}
    public static String resolve(String key, Map<String, String> player, Map<String, String> viewer,
            Map<String, String> global, String fallback) {
        String value = player.get(key);
        if (value == null) value = global.get(key);
        if (value != null) return value;
        return switch (key) {
            case "my_ping" -> viewer.getOrDefault("ping", "0");
            case "local_time", "local_time_s" -> global.getOrDefault("time", "");
            case "local_date" -> global.getOrDefault("date", "");
            default -> ServerValues.CLIENT_ONLY.contains(key) ? fallback : "";
        };
    }
}
