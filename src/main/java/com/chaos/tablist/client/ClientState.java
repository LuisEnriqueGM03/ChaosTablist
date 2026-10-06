package com.chaos.tablist.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.chaos.tablist.ChaosTablist;
import com.chaos.tablist.config.TabConfig;
import com.chaos.tablist.net.TabPayloads;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Lo que el servidor manda al cliente con mod: la configuración y los valores de los placeholders. */
public final class ClientState {

	@Nullable private static TabConfig config;
	private static Map<String, String> global = new HashMap<>();
	private static Map<UUID, Map<String, String>> players = new HashMap<>();

	private ClientState() {}

	/** null = el servidor no tiene el mod (o aún no ha mandado nada): se usa el Tab vanilla. */
	@Nullable
	public static TabConfig config() {
		return config;
	}

	public static Map<String, String> global() {
		return global;
	}

	public static Map<String, String> player(UUID id) {
		return players.getOrDefault(id, Map.of());
	}

	public static void setConfig(byte[] data) {
		try {
			config = TabConfig.fromJson(TabPayloads.gunzip(data));
		} catch (Exception e) {
			ChaosTablist.LOGGER.error("Configuración del Tab inválida", e);
		}
	}

	public static void setValues(byte[] data) {
		try {
			JsonObject root = JsonParser.parseString(TabPayloads.gunzip(data)).getAsJsonObject();
			Map<String, String> g = new HashMap<>();
			for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("g").entrySet()) {
				g.put(e.getKey(), e.getValue().getAsString());
			}
			Map<UUID, Map<String, String>> p = new HashMap<>();
			for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("p").entrySet()) {
				Map<String, String> values = new HashMap<>();
				for (Map.Entry<String, JsonElement> v : e.getValue().getAsJsonObject().entrySet()) {
					values.put(v.getKey(), v.getValue().getAsString());
				}
				p.put(UUID.fromString(e.getKey()), values);
			}
			global = g;
			players = p;
		} catch (Exception e) {
			ChaosTablist.LOGGER.error("Valores del Tab inválidos", e);
		}
	}

	public static void reset() {
		config = null;
		global = new HashMap<>();
		players = new HashMap<>();
	}
}
