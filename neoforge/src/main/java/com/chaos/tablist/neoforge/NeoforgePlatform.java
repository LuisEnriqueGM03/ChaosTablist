package com.chaos.tablist.neoforge;

import java.nio.file.Path;
import com.chaos.tablist.platform.Platform;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class NeoforgePlatform implements Platform {
    public String name() { return "neoforge"; }
    public Path configDirectory() { return FMLPaths.CONFIGDIR.get(); }
    public boolean isModLoaded(String id) { return ModList.get().isLoaded(id); }
    public int modCount() { return ModList.get().getMods().size(); }
    public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return player.connection.hasChannel(type.id());
    }
    public void send(ServerPlayer player, CustomPacketPayload payload) { PacketDistributor.sendToPlayer(player, payload); }
    public void sendToServer(CustomPacketPayload payload) { net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload); }
}
