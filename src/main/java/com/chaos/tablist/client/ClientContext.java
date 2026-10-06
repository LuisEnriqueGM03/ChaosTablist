package com.chaos.tablist.client;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.chaos.tablist.config.TabConfig;
import com.chaos.tablist.text.TextContext;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;

/**
 * Contexto del cliente con mod: valores del jugador → globales del servidor → valores del propio cliente.
 * {@code vanilla} = simular lo que vería un cliente sin el mod (vista previa del editor).
 */
public record ClientContext(TabConfig config, Map<String, String> player, Map<String, String> global, double time,
		double openedAt, boolean vanilla) implements TextContext {

	@Override
	public String value(String key) {
		String v = player.get(key);
		if (v == null) {
			v = global.get(key);
		}
		if (v == null) {
			v = ClientValues.get().get(key);
		}
		return v == null ? "" : v;
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

	@Nullable
	@Override
	public HolderLookup.Provider registries() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null ? mc.level.registryAccess() : null;
	}

	public static double now() {
		return System.currentTimeMillis() / 1000.0;
	}
}
