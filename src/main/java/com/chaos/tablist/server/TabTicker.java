package com.chaos.tablist.server;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.chaos.tablist.config.TabConfig;
import com.chaos.tablist.config.TabStore;
import com.chaos.tablist.net.TabPayloads;
import com.chaos.tablist.text.Evaluator;
import com.chaos.tablist.text.Glyph;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Corre cada tick del servidor:
 * <ul>
 *   <li>Cada segundo recalcula los valores y los manda a los clientes con mod.</li>
 *   <li>Cada {@code updateTicks} monta el Tab de los clientes vanilla (header, footer y nombres) y solo manda
 *       lo que cambió.</li>
 * </ul>
 * Los clientes con mod no reciben nada de esto: animan el Tab ellos mismos a cada frame.
 */
public final class TabTicker {

	private static final Set<UUID> MODDED = new HashSet<>();
	/** Nombre del Tab de cada jugador (lo devuelve ServerPlayer#getTabListDisplayName por el mixin). */
	private static final Map<UUID, Component> DISPLAY_NAMES = new HashMap<>();
	private static final Map<UUID, Component[]> LAST_HEADER = new HashMap<>();
	private static int ticks;
	private static boolean wasEnabled = true;

	private TabTicker() {}

	@Nullable
	public static Component displayName(UUID id) {
		return DISPLAY_NAMES.get(id);
	}

	public static boolean modded(ServerPlayer player) {
		return MODDED.contains(player.getUUID());
	}

	public static void join(ServerPlayer player) {
		// -Dchaostablist.forceVanilla=true: tratar a todos como clientes sin mod (para probar lo que ven).
		if (ServerPlayNetworking.canSend(player, TabPayloads.SyncConfig.TYPE) && !Boolean.getBoolean("chaostablist.forceVanilla")) {
			MODDED.add(player.getUUID());
			TabServer.sendIcons(player);
			TabServer.sendConfig(player);
		}
		// Valores al momento: así el que entra ya tiene su rango y sus datos en el primer frame.
		ServerValues.update(player.server, TabStore.config());
		broadcastValues(player.server);
	}

	public static void leave(ServerPlayer player) {
		MODDED.remove(player.getUUID());
		DISPLAY_NAMES.remove(player.getUUID());
		LAST_HEADER.remove(player.getUUID());
	}

	public static void stop() {
		MODDED.clear();
		DISPLAY_NAMES.clear();
		LAST_HEADER.clear();
		ticks = 0;
	}

	/** Tras recargar o guardar la configuración: todo se vuelve a mandar. */
	public static void invalidate() {
		LAST_HEADER.clear();
	}

	public static void tick(MinecraftServer server) {
		TabConfig config = TabStore.config();
		ticks++;
		if (ticks % 20 == 0) {
			ServerValues.update(server, config);
			broadcastValues(server);
		}
		if (!config.enabled) {
			if (wasEnabled) {
				disable(server);
			}
			wasEnabled = false;
			return;
		}
		wasEnabled = true;
		if (ticks % config.updateTicks == 0) {
			updateVanilla(server, config);
		}
	}

	private static void broadcastValues(MinecraftServer server) {
		if (MODDED.isEmpty()) {
			return;
		}
		TabPayloads.SyncValues payload = new TabPayloads.SyncValues(TabPayloads.gzip(ServerValues.toJson()));
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (MODDED.contains(p.getUUID())) {
				ServerPlayNetworking.send(p, payload);
			}
		}
	}

	private static void updateVanilla(MinecraftServer server, TabConfig config) {
		List<ServerPlayer> all = server.getPlayerList().getPlayers();
		List<ServerPlayer> vanilla = all.stream().filter(p -> !MODDED.contains(p.getUUID())).toList();
		if (vanilla.isEmpty()) {
			return;
		}
		double now = ServerContext.now();

		// Nombres: iguales para todos los que miran.
		List<ServerPlayer> changed = new ArrayList<>();
		for (ServerPlayer p : all) {
			Map<String, String> values = ServerValues.player(p.getUUID());
			ServerContext ctx = new ServerContext(config, values, values, server.registryAccess(), now);
			String template = config.rowTemplate(p.getStringUUID()) + config.ping.vanillaSuffix;
			Component name = Evaluator.toComponent(Evaluator.eval(template, ctx));
			if (!name.equals(DISPLAY_NAMES.get(p.getUUID()))) {
				DISPLAY_NAMES.put(p.getUUID(), name);
				changed.add(p);
			}
		}
		if (!changed.isEmpty()) {
			ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
					EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME), changed);
			vanilla.forEach(p -> p.connection.send(packet));
		}

		// Header y footer: distintos para cada uno (su ping, su mundo...).
		for (ServerPlayer viewer : vanilla) {
			Map<String, String> values = ServerValues.player(viewer.getUUID());
			ServerContext ctx = new ServerContext(config, values, values, server.registryAccess(), now);
			Component header = lines(config.header, ctx);
			Component footer = lines(config.footer, ctx);
			Component[] last = LAST_HEADER.get(viewer.getUUID());
			if (last == null || !Objects.equals(last[0], header) || !Objects.equals(last[1], footer)) {
				LAST_HEADER.put(viewer.getUUID(), new Component[] {header, footer});
				viewer.connection.send(new ClientboundTabListPacket(header, footer));
			}
		}
	}

	private static Component lines(List<String> lines, ServerContext ctx) {
		MutableComponent out = Component.empty();
		for (int i = 0; i < lines.size(); i++) {
			if (i > 0) {
				out.append("\n");
			}
			List<Glyph> glyphs = Evaluator.eval(lines.get(i), ctx);
			out.append(Evaluator.toComponent(glyphs));
		}
		return out;
	}

	/** Tab desactivado: los clientes vanilla vuelven a verlo como siempre. */
	private static void disable(MinecraftServer server) {
		List<ServerPlayer> all = server.getPlayerList().getPlayers();
		DISPLAY_NAMES.clear();
		LAST_HEADER.clear();
		ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
				EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME), all);
		for (ServerPlayer p : all) {
			if (!MODDED.contains(p.getUUID())) {
				p.connection.send(packet);
				p.connection.send(new ClientboundTabListPacket(Component.empty(), Component.empty()));
			}
		}
	}
}
