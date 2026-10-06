package com.chaos.tablist.server;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import com.chaos.tablist.compat.ChaosRanksCompat;
import com.chaos.tablist.config.TabConfig;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;

/**
 * Orden del Tab en los clientes vanilla: ordenan por nombre de equipo, así que cada grupo tiene su equipo
 * "ctab_&lt;orden&gt;_&lt;grupo&gt;". No toca a quien ya está en otro equipo, y no hace nada si Chaos Ranks está
 * (sus equipos ya ordenan por rango). Los clientes con mod ordenan ellos mismos.
 */
public final class TabSorting {

	public static final String PREFIX = "ctab_";

	private TabSorting() {}

	public static void update(MinecraftServer server, TabConfig config) {
		if (!config.enabled || !config.sorting.vanillaTeams || ChaosRanksCompat.present()) {
			return;
		}
		ServerScoreboard board = server.getScoreboard();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			PlayerTeam current = p.getTeam();
			if (current != null && !current.getName().startsWith(PREFIX)) {
				continue;
			}
			Map<String, String> v = ServerValues.player(p.getUUID());
			String group = v.getOrDefault("group", "default");
			int priority = parse(v.get("group_priority"));
			String name = String.format(Locale.ROOT, "%s%04d_%s", PREFIX, 5000 - priority, group);
			if (current != null && current.getName().equals(name)) {
				continue;
			}
			PlayerTeam team = board.getPlayerTeam(name);
			if (team == null) {
				team = board.addPlayerTeam(name);
			}
			board.addPlayerToTeam(p.getScoreboardName(), team);
		}
		// Equipos del mod que se quedaron vacíos.
		for (PlayerTeam team : new ArrayList<>(board.getPlayerTeams())) {
			if (team.getName().startsWith(PREFIX) && team.getPlayers().isEmpty()) {
				board.removePlayerTeam(team);
			}
		}
	}

	public static void clear(MinecraftServer server) {
		ServerScoreboard board = server.getScoreboard();
		for (PlayerTeam team : new ArrayList<>(board.getPlayerTeams())) {
			if (team.getName().startsWith(PREFIX)) {
				board.removePlayerTeam(team);
			}
		}
	}

	private static int parse(String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException | NullPointerException e) {
			return 0;
		}
	}
}
