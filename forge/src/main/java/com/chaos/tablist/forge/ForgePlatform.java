package com.chaos.tablist.forge;

import java.nio.file.Path;
import com.chaos.tablist.platform.Platform;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class ForgePlatform implements Platform {
    public String name() { return "forge"; }
    public Path configDirectory() { return FMLPaths.CONFIGDIR.get(); }
    public boolean isModLoaded(String id) { return ModList.isLoaded(id); }
    public int modCount() { return ModList.getMods().size(); }
    public boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
        return ForgeNetwork.CHANNEL.isRemotePresent(player.connection.getConnection());
    }
    public void send(ServerPlayer player, CustomPacketPayload payload) { ForgeNetwork.CHANNEL.send(payload, PacketDistributor.PLAYER.with(player)); }
    public void sendToServer(CustomPacketPayload payload) { ForgeNetwork.CHANNEL.send(payload, PacketDistributor.SERVER.noArg()); }
}
