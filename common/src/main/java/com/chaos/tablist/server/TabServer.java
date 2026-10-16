package com.chaos.tablist.server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.chaos.tablist.ChaosTablist;
import com.chaos.tablist.Lang;
import com.chaos.tablist.compat.ChaosRanksCompat;
import com.chaos.tablist.config.TabConfig;
import com.chaos.tablist.config.TabStore;
import com.chaos.tablist.net.TabPayloads;
import com.chaos.tablist.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Permission-checked, revisioned editor saves. Live state changes only after disk persistence succeeds. */
public final class TabServer {
    public static final int PERMISSION = 2;
    private static final Map<UUID, Long> LAST_SAVE = new HashMap<>();
    private static TabPayloads.SyncConfig cachedConfig;
    private static long cachedRevision = -1;
    private TabServer() {}

    public static void clear() { LAST_SAVE.clear(); cachedConfig = null; cachedRevision = -1; }
    public static void leave(UUID id) { LAST_SAVE.remove(id); }


    public static void open(ServerPlayer player) {
        if (!player.hasPermissions(PERMISSION)) return;
        if (!Services.PLATFORM.canSend(player, TabPayloads.OpenEditor.TYPE)
                || !Services.PLATFORM.canSend(player, TabPayloads.SaveResult.TYPE)) {
            player.sendSystemMessage(Lang.tr("msg.need_client").withStyle(ChatFormatting.RED));
            return;
        }
        Services.PLATFORM.send(player, new TabPayloads.OpenEditor(TabPayloads.gzip(TabStore.config().toJson()),
            ChaosRanksCompat.present(), TabStore.revision()));
    }

    private static void result(ServerPlayer player, TabPayloads.SaveConfig request, boolean success, String reason) {
        Services.PLATFORM.send(player, new TabPayloads.SaveResult(request.requestId(), success, reason, TabStore.revision()));
    }

    public static void save(ServerPlayer player, TabPayloads.SaveConfig payload) {
        if (!player.hasPermissions(PERMISSION)) { result(player, payload, false, "permission"); return; }
        long now = net.minecraft.Util.getMillis();
        Long last = LAST_SAVE.put(player.getUUID(), now);
        if (last != null && now - last < 500) { result(player, payload, false, "rate_limit"); return; }
        if (payload.revision() != TabStore.revision()) { result(player, payload, false, "conflict"); return; }
        TabConfig config;
        try {
            config = TabConfig.fromJson(TabPayloads.gunzip(payload.data()));
        } catch (Exception e) {
            ChaosTablist.LOGGER.warn("Configuración inválida de {}: {}", player.getScoreboardName(), e.getMessage());
            result(player, payload, false, "invalid");
            return;
        }
        if (!apply(player.server, config)) { result(player, payload, false, "disk"); return; }
        result(player, payload, true, "saved");
        ChaosTablist.LOGGER.info("{} guardó la configuración del Tab (revisión {})", player.getScoreboardName(), TabStore.revision());
    }

    public static boolean apply(MinecraftServer server, TabConfig config) {
        if (!TabStore.set(config)) return false;
        refresh(server);
        return true;
    }

    private static void refresh(MinecraftServer server) {
        TabTicker.invalidate();
        ServerValues.update(server, TabStore.config());
        broadcastConfig(server);
    }

    public static boolean reload(MinecraftServer server) {
        if (!TabStore.load(server)) return false;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) if (TabTicker.modded(p)) sendIcons(p);
        refresh(server);
        return true;
    }

    private static TabPayloads.SyncConfig configPayload() {
        if (cachedConfig == null || cachedRevision != TabStore.revision()) {
            cachedRevision = TabStore.revision();
            cachedConfig = new TabPayloads.SyncConfig(TabPayloads.gzip(TabStore.config().toJson()), cachedRevision);
        }
        return cachedConfig;
    }

    public static void broadcastConfig(MinecraftServer server) {
        var payload = configPayload();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) if (TabTicker.modded(p)) Services.PLATFORM.send(p, payload);
        TabTicker.broadcastValues(server);
    }

    public static void sendConfig(ServerPlayer player) {
        Services.PLATFORM.send(player, configPayload());
        Services.PLATFORM.send(player, new TabPayloads.SyncValues(TabPayloads.gzip(ServerValues.toJson())));
        sendSelfValues(player);
    }

    public static void sendSelfValues(ServerPlayer player) {
        Services.PLATFORM.send(player, new TabPayloads.SelfValues(TabPayloads.gzip(ServerValues.selfJson(player.getUUID()))));
    }

    public static void sendIcons(ServerPlayer player) {
        Services.PLATFORM.send(player, new TabPayloads.IconData("*", new byte[0]));
        for (Map.Entry<String, byte[]> e : TabStore.icons().entrySet())
            Services.PLATFORM.send(player, new TabPayloads.IconData(e.getKey(), e.getValue()));
    }
}
