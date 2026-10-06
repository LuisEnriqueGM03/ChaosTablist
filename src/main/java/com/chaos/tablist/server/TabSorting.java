package com.chaos.tablist.server;

import java.util.ArrayList;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.PlayerTeam;

/**
 * Las versiones con grupos ordenaban a los clientes vanilla con equipos "ctab_*". Ya no se crean: al arrancar se
 * borran los que hayan quedado en el mundo. El orden por rango lo hacen los equipos de Chaos Ranks.
 */
public final class TabSorting {

	public static final String PREFIX = "ctab_";

	private TabSorting() {}

	public static void clear(MinecraftServer server) {
		ServerScoreboard board = server.getScoreboard();
		for (PlayerTeam team : new ArrayList<>(board.getPlayerTeams())) {
			if (team.getName().startsWith(PREFIX)) {
				board.removePlayerTeam(team);
			}
		}
	}
}
