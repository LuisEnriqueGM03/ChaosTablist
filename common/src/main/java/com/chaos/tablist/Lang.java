package com.chaos.tablist;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Textos traducibles (assets/chaostablist/lang). El cliente los muestra en su idioma; el texto en inglés va
 * como respaldo para quien no tenga el mod o para la consola del servidor.
 */
public final class Lang {

	private static final Map<String, String> ENGLISH = new HashMap<>();

	static {
		try (var in = Lang.class.getResourceAsStream("/assets/chaostablist/lang/en_us.json")) {
			for (Map.Entry<String, JsonElement> e : JsonParser.parseReader(
					new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().entrySet()) {
				ENGLISH.put(e.getKey(), e.getValue().getAsString());
			}
		} catch (Exception e) {
			ChaosTablist.LOGGER.error("No se pudo leer en_us.json", e);
		}
	}

	private Lang() {}

	public static MutableComponent tr(String key, Object... args) {
		return Component.translatableWithFallback("chaostablist." + key, ENGLISH.get("chaostablist." + key), args);
	}

	public static MutableComponent yesNo(boolean value) {
		return tr(value ? "yes" : "no");
	}
}
