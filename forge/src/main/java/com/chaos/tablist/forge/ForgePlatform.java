package com.chaos.tablist.forge;

import java.nio.file.Path;
import com.chaos.tablist.platform.Platform;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;
import com.chaos.tablist.net.TabPayloads.Message;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ForgePlatform implements Platform {
    public String name() { return "forge"; }
    public Path configDirectory() { return FMLPaths.CONFIGDIR.get(); }
    public boolean isModLoaded(String id) { return ModList.get().isLoaded(id); }
    public int modCount() { return ModList.get().getMods().size(); }
    public boolean canSend(ServerPlayer player, ResourceLocation type) {
        return ForgeNetwork.CHANNEL.isRemotePresent(player.connection.connection);
    }
    public void send(ServerPlayer player, Message payload) { ForgeNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload); }
    public void sendToServer(Message payload) { ForgeNetwork.CHANNEL.sendToServer(payload); }
}
