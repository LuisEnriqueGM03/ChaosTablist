package com.chaos.tablist.server;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.chaos.tablist.compat.ChaosRanksCompat;
import com.chaos.tablist.config.TabConfig;
import com.chaos.tablist.text.TextContext;
import com.google.gson.JsonObject;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.scores.PlayerTeam;

/**
 * Valores de los placeholders, recalculados una vez por segundo: los globales (TPS, RAM...) y los de cada
 * jugador (ping, mundo, rango...). Son la única fuente: el servidor evalúa con ellos para los clientes vanilla
 * y se mandan tal cual a los clientes con mod.
 */
public final class ServerValues {

	/** Placeholders que solo conoce el cliente (con el mod se resuelven allí). */
	public static final Set<String> CLIENT_ONLY = Set.of("fps", "client_ram_used", "client_ram_max", "client_ram_pct",
			"client_ram", "client_cpu", "gpu");

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

	private static Map<String, String> global = new HashMap<>();
	private static Map<UUID, Map<String, String>> players = new HashMap<>();
	private static long startedAt = System.currentTimeMillis();

	private ServerValues() {}

	public static void markStarted() {
		startedAt = System.currentTimeMillis();
	}

	public static Map<String, String> global() {
		return global;
	}

	public static Map<String, String> player(UUID id) {
		return players.getOrDefault(id, Map.of());
	}

	public static void update(MinecraftServer server, TabConfig config) {
		Map<String, String> g = new HashMap<>();
		g.put("server_name", config.serverName);
		g.put("motd", server.getMotd());
		g.put("version", SharedConstants.getCurrentVersion().getName());
		g.put("online", String.valueOf(server.getPlayerCount()));
		g.put("max_players", String.valueOf(server.getMaxPlayers()));

		float mspt = server.getCurrentSmoothedTickTime();
		float rate = server.tickRateManager().tickrate();
		float tps = mspt <= 0 ? rate : Math.min(rate, 1000f / mspt);
		g.put("tps", String.format(Locale.ROOT, "%.1f", tps));
		g.put("mspt", String.format(Locale.ROOT, "%.1f", mspt));

		Runtime rt = Runtime.getRuntime();
		long used = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
		long max = rt.maxMemory() / (1024 * 1024);
		g.put("ram_used", String.valueOf(used));
		g.put("ram_max", String.valueOf(max));
		g.put("ram_pct", String.valueOf(max == 0 ? 0 : used * 100 / max));
		g.put("ram", formatMemory(used, max));
		g.put("cpu", cpu());

		long up = (System.currentTimeMillis() - startedAt) / 1000;
		g.put("uptime", duration(up));

		ServerLevel overworld = server.overworld();
		long dayTime = overworld.getDayTime();
		g.put("day", String.valueOf(dayTime / 24000));
		long ticksOfDay = dayTime % 24000;
		long hours = (ticksOfDay / 1000 + 6) % 24;
		long minutes = ticksOfDay % 1000 * 60 / 1000;
		g.put("world_time", String.format(Locale.ROOT, "%02d:%02d", hours, minutes));
		String weather = overworld.isThundering() ? "thunder" : overworld.isRaining() ? "rain" : "clear";
		g.put("weather", weather);
		g.put("weather_icon", switch (weather) {
			case "thunder" -> "⚡";
			case "rain" -> "☂";
			default -> "☀";
		});
		LocalDateTime now = LocalDateTime.now();
		g.put("date", now.format(DATE));
		g.put("time", now.format(TIME));
		g.put("chaosranks", String.valueOf(ChaosRanksCompat.present()));

		Map<UUID, Map<String, String>> p = new HashMap<>();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			p.put(player.getUUID(), playerValues(server, config, player));
		}
		global = g;
		players = p;
	}

	private static Map<String, String> playerValues(MinecraftServer server, TabConfig config, ServerPlayer player) {
		Map<String, String> v = new HashMap<>();
		v.put("player", player.getScoreboardName());
		v.put("uuid", player.getStringUUID());
		v.put("ping", String.valueOf(player.connection.latency()));
		ServerLevel level = player.serverLevel();
		v.put("world", level.dimension().location().getPath());
		v.put("dimension", level.dimension().location().toString());
		v.put("biome", level.getBiome(player.blockPosition()).unwrapKey()
				.map(k -> k.location().getPath()).orElse("unknown"));
		v.put("x", String.valueOf(player.getBlockX()));
		v.put("y", String.valueOf(player.getBlockY()));
		v.put("z", String.valueOf(player.getBlockZ()));
		v.put("health", String.valueOf((int) Math.ceil(player.getHealth())));
		v.put("max_health", String.valueOf((int) Math.ceil(player.getMaxHealth())));
		v.put("food", String.valueOf(player.getFoodData().getFoodLevel()));
		v.put("level", String.valueOf(player.experienceLevel));
		v.put("gamemode", player.gameMode.getGameModeForPlayer().getName());
		v.put("deaths", String.valueOf(player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS))));
		v.put("kills", String.valueOf(player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS))));
		v.put("mob_kills", String.valueOf(player.getStats().getValue(Stats.CUSTOM.get(Stats.MOB_KILLS))));
		v.put("playtime", duration(player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) / 20L));
		long idle = net.minecraft.Util.getMillis() - player.getLastActionTime();
		v.put("afk", String.valueOf(idle > 5 * 60 * 1000));
		v.put("op", String.valueOf(server.getPlayerList().isOp(player.getGameProfile())));
		PlayerTeam team = player.getTeam();
		v.put("team", team == null ? "" : team.getName());

		ChaosRanksCompat.Rank rank = ChaosRanksCompat.rankOf(player);
		v.put("has_rank", String.valueOf(rank != null));
		v.put("rank", rank == null ? "" : TextContext.JSON_MARK + Component.Serializer.toJson(rank.badge(), server.registryAccess()));
		v.put("rank_name", rank == null ? "" : rank.label());
		v.put("rank_id", rank == null ? "" : rank.id());
		v.put("rank_priority", String.valueOf(rank == null ? 0 : rank.priority()));

		TabConfig.GroupDef group = Groups.match(server, config, player, rank);
		v.put("group", group == null ? "" : group.id);
		v.put("group_priority", String.valueOf(group == null ? 0 : group.priority));
		return v;
	}

	/** {"g": {...}, "p": {uuid: {...}}} para los clientes con mod. */
	public static String toJson() {
		JsonObject root = new JsonObject();
		JsonObject g = new JsonObject();
		global.forEach(g::addProperty);
		root.add("g", g);
		JsonObject p = new JsonObject();
		players.forEach((id, values) -> {
			JsonObject o = new JsonObject();
			values.forEach(o::addProperty);
			p.add(id.toString(), o);
		});
		root.add("p", p);
		return root.toString();
	}

	public static String formatMemory(long usedMb, long maxMb) {
		if (maxMb >= 1024) {
			return String.format(Locale.ROOT, "%.1f/%.1f GB", usedMb / 1024f, maxMb / 1024f);
		}
		return usedMb + "/" + maxMb + " MB";
	}

	public static String duration(long seconds) {
		long d = seconds / 86400;
		long h = seconds % 86400 / 3600;
		long m = seconds % 3600 / 60;
		if (d > 0) {
			return d + "d " + h + "h";
		}
		if (h > 0) {
			return h + "h " + m + "m";
		}
		return m + "m " + seconds % 60 + "s";
	}

	private static String cpu() {
		try {
			if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os) {
				double load = os.getProcessCpuLoad();
				return load < 0 ? "0" : String.valueOf(Math.round(load * 100));
			}
		} catch (Throwable ignored) {
			// No todas las JVM lo tienen.
		}
		return "0";
	}
}
