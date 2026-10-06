package com.chaos.tablist.server;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.chaos.tablist.compat.ChaosRanksCompat;
import com.chaos.tablist.config.TabConfig;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;

/** Elige el grupo de cada jugador: el de más prioridad cuya condición cumple. */
public final class Groups {

	private Groups() {}

	@Nullable
	public static TabConfig.GroupDef match(MinecraftServer server, TabConfig config, ServerPlayer player,
			@Nullable ChaosRanksCompat.Rank rank) {
		for (TabConfig.GroupDef g : config.groups) {
			if (matches(server, g.condition, player, rank)) {
				return g;
			}
		}
		return null;
	}

	/** Condiciones separadas por coma: todas tienen que cumplirse. "!" delante = negada. */
	public static boolean matches(MinecraftServer server, String condition, ServerPlayer player,
			@Nullable ChaosRanksCompat.Rank rank) {
		for (String raw : condition.split(",")) {
			String c = raw.trim();
			if (c.isEmpty()) {
				continue;
			}
			boolean negate = c.startsWith("!");
			if (negate) {
				c = c.substring(1).trim();
			}
			if (one(server, c, player, rank) == negate) {
				return false;
			}
		}
		return true;
	}

	private static boolean one(MinecraftServer server, String c, ServerPlayer player, @Nullable ChaosRanksCompat.Rank rank) {
		int colon = c.indexOf(':');
		String key = (colon < 0 ? c : c.substring(0, colon)).toLowerCase(Locale.ROOT);
		String value = colon < 0 ? "" : c.substring(colon + 1).trim();
		return switch (key) {
			case "default", "all", "*" -> true;
			case "op" -> server.getPlayerList().isOp(player.getGameProfile());
			case "permission", "perm", "level" -> player.hasPermissions(parse(value, 2));
			case "rank" -> rank != null && rank.id().equalsIgnoreCase(value);
			case "gamemode", "gm" -> player.gameMode.getGameModeForPlayer().getName().equalsIgnoreCase(value);
			case "dimension", "world" -> {
				String dim = player.serverLevel().dimension().location().toString();
				yield dim.equalsIgnoreCase(value) || player.serverLevel().dimension().location().getPath().equalsIgnoreCase(value);
			}
			case "team" -> {
				PlayerTeam team = player.getTeam();
				yield team != null && team.getName().equalsIgnoreCase(value);
			}
			case "player", "name" -> player.getScoreboardName().equalsIgnoreCase(value);
			case "tag" -> player.getTags().contains(value);
			default -> false;
		};
	}

	private static int parse(String s, int def) {
		try {
			return Integer.parseInt(s.trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
