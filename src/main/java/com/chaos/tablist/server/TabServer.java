package com.chaos.tablist.server;

import java.util.Map;

import com.chaos.tablist.ChaosTablist;
import com.chaos.tablist.Lang;
import com.chaos.tablist.compat.ChaosRanksCompat;
import com.chaos.tablist.config.TabConfig;
import com.chaos.tablist.config.TabStore;
import com.chaos.tablist.net.TabPayloads;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Lado servidor del editor y de la sincronización de la configuración con los clientes con mod. */
public final class TabServer {

	public static final int PERMISSION = 2;

	private TabServer() {}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(TabPayloads.SaveConfig.TYPE, (payload, context) ->
				context.server().execute(() -> save(context.player(), payload)));
	}

	public static void open(ServerPlayer player) {
		if (!ServerPlayNetworking.canSend(player, TabPayloads.OpenEditor.TYPE)) {
			player.sendSystemMessage(Lang.tr("msg.need_client").withStyle(ChatFormatting.RED));
			return;
		}
		ServerPlayNetworking.send(player, new TabPayloads.OpenEditor(TabPayloads.gzip(TabStore.config().toJson()),
				ChaosRanksCompat.present()));
	}

	private static void save(ServerPlayer player, TabPayloads.SaveConfig payload) {
		if (!player.hasPermissions(PERMISSION)) {
			return;
		}
		TabConfig config;
		try {
			config = TabConfig.fromJson(TabPayloads.gunzip(payload.data()));
		} catch (Exception e) {
			ChaosTablist.LOGGER.warn("Configuración inválida de {}", player.getScoreboardName(), e);
			player.sendSystemMessage(Lang.tr("msg.invalid").withStyle(ChatFormatting.RED));
			return;
		}
		apply(player.server, config);
		ChaosTablist.LOGGER.info("{} guardó la configuración del Tab", player.getScoreboardName());
		player.sendSystemMessage(Lang.tr("msg.saved").withStyle(ChatFormatting.GREEN));
	}

	/** Guarda, recalcula y manda la configuración nueva a todos (en caliente). */
	public static void apply(MinecraftServer server, TabConfig config) {
		TabStore.set(config);
		TabTicker.invalidate();
		ServerValues.update(server, TabStore.config());
		broadcastConfig(server);
	}

	public static void reload(MinecraftServer server) {
		TabStore.load(server);
		TabTicker.invalidate();
		ServerValues.update(server, TabStore.config());
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (TabTicker.modded(p)) {
				sendIcons(p);
			}
		}
		broadcastConfig(server);
	}

	public static void broadcastConfig(MinecraftServer server) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (TabTicker.modded(p)) {
				sendConfig(p);
			}
		}
	}

	public static void sendConfig(ServerPlayer player) {
		ServerPlayNetworking.send(player, new TabPayloads.SyncConfig(TabPayloads.gzip(TabStore.config().toJson())));
		ServerPlayNetworking.send(player, new TabPayloads.SyncValues(TabPayloads.gzip(ServerValues.toJson())));
	}

	public static void sendIcons(ServerPlayer player) {
		ServerPlayNetworking.send(player, new TabPayloads.IconData("*", new byte[0]));
		for (Map.Entry<String, byte[]> e : TabStore.icons().entrySet()) {
			ServerPlayNetworking.send(player, new TabPayloads.IconData(e.getKey(), e.getValue()));
		}
	}
}
