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
        return com.chaos.tablist.text.VanillaValues.resolve(key, player, viewer, ServerValues.global(), config.clientOnlyFallback);
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
