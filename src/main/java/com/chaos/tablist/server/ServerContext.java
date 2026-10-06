package com.chaos.tablist.server;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.chaos.tablist.config.TabConfig;
import com.chaos.tablist.text.TextContext;

import net.minecraft.core.HolderLookup;

/** Contexto para montar el Tab de un cliente vanilla: valores del jugador, luego los globales. */
public record ServerContext(TabConfig config, Map<String, String> player, Map<String, String> viewer,
		HolderLookup.Provider registries, double time) implements TextContext {

	@Override
	public String value(String key) {
		String v = player.get(key);
		if (v == null) {
			v = ServerValues.global().get(key);
		}
		if (v != null) {
			return v;
		}
		// Lo que sin mod no se puede saber del cliente.
		return switch (key) {
			case "my_ping" -> viewer.getOrDefault("ping", "0");
			case "local_time" -> ServerValues.global().getOrDefault("time", "");
			case "local_date" -> ServerValues.global().getOrDefault("date", "");
			default -> ServerValues.CLIENT_ONLY.contains(key) ? config.clientOnlyFallback : "";
		};
	}

	@Override
	public boolean vanilla() {
		return true;
	}

	@Nullable
	@Override
	public String animationFrame(String name, double time) {
		return config.animationFrame(name, time);
	}

	@Override
	public int pingColor(int ping) {
		return config.ping.color(ping);
	}

	public static double now() {
		return System.currentTimeMillis() / 1000.0;
	}
}
