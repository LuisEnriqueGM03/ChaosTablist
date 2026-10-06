package com.chaos.tablist.client;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.chaos.tablist.server.ServerValues;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

/**
 * Placeholders que solo conoce el cliente: FPS, su RAM, su ping, su hora, distancia de render, resolución,
 * gráfica... Se recalculan 4 veces por segundo.
 */
public final class ClientValues {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter TIME_S = DateTimeFormatter.ofPattern("HH:mm:ss");

	private static Map<String, String> values = new HashMap<>();
	private static long lastUpdate;

	private ClientValues() {}

	public static Map<String, String> get() {
		long now = Util.getMillis();
		if (now - lastUpdate > 250) {
			lastUpdate = now;
			values = compute();
		}
		return values;
	}

	private static Map<String, String> compute() {
		Minecraft mc = Minecraft.getInstance();
		Map<String, String> v = new HashMap<>();
		v.put("fps", String.valueOf(mc.getFps()));
		Runtime rt = Runtime.getRuntime();
		long used = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
		long max = rt.maxMemory() / (1024 * 1024);
		v.put("client_ram_used", String.valueOf(used));
		v.put("client_ram_max", String.valueOf(max));
		v.put("client_ram_pct", String.valueOf(max == 0 ? 0 : used * 100 / max));
		v.put("client_ram", ServerValues.formatMemory(used, max));
		LocalDateTime now = LocalDateTime.now();
		v.put("local_time", now.format(TIME));
		v.put("local_time_s", now.format(TIME_S));
		v.put("local_date", now.format(DATE));
		int ping = 0;
		if (mc.player != null && mc.getConnection() != null) {
			PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
			ping = info == null ? 0 : info.getLatency();
		}
		v.put("my_ping", String.valueOf(ping));
		v.put("render_distance", String.valueOf(mc.options.getEffectiveRenderDistance()));
		v.put("resolution", mc.getWindow().getWidth() + "x" + mc.getWindow().getHeight());
		v.put("client_mods", String.valueOf(FabricLoader.getInstance().getAllMods().size()));
		v.put("gpu", gpu());
		Locale locale = Locale.forLanguageTag(mc.options.languageCode.replace('_', '-'));
		String day = now.getDayOfWeek().getDisplayName(TextStyle.FULL, locale);
		v.put("local_weekday", day.isEmpty() ? day : day.substring(0, 1).toUpperCase(locale) + day.substring(1));
		return v;
	}

	private static String gpu;

	/** Nombre corto de la tarjeta gráfica (sin "/PCIe/SSE2" y demás). */
	private static String gpu() {
		if (gpu == null) {
			String r = com.mojang.blaze3d.platform.GlUtil.getRenderer();
			int cut = r.indexOf('/');
			gpu = (cut > 0 ? r.substring(0, cut) : r).trim();
		}
		return gpu;
	}
}
