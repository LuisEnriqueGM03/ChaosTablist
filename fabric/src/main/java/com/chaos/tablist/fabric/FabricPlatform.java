package com.chaos.tablist.fabric;

import java.nio.file.Path;
import com.chaos.tablist.platform.Platform;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class FabricPlatform implements Platform {
    public String name() { return "fabric"; }
    public Path configDirectory() { return FabricLoader.getInstance().getConfigDir(); }
    public boolean isModLoaded(String id) { return FabricLoader.getInstance().isModLoaded(id); }
    public int modCount() { return FabricLoader.getInstance().getAllMods().size(); }
    public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) { return ServerPlayNetworking.canSend(player, type); }
    public void send(ServerPlayer player, CustomPacketPayload payload) { ServerPlayNetworking.send(player, payload); }
    public void sendToServer(CustomPacketPayload payload) { ClientPlayNetworking.send(payload); }
}
