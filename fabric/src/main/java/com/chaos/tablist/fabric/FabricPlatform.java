package com.chaos.tablist.fabric;

import java.nio.file.Path;
import com.chaos.tablist.platform.Platform;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.chaos.tablist.net.TabPayloads.Message;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class FabricPlatform implements Platform {
    public String name() { return "fabric"; }
    public Path configDirectory() { return FabricLoader.getInstance().getConfigDir(); }
    public boolean isModLoaded(String id) { return FabricLoader.getInstance().isModLoaded(id); }
    public int modCount() { return FabricLoader.getInstance().getAllMods().size(); }
    public boolean canSend(ServerPlayer player, ResourceLocation type) { return ServerPlayNetworking.canSend(player, type); }
    public void send(ServerPlayer player, Message payload) { ServerPlayNetworking.send(player, payload.type(), encode(payload)); }
    public void sendToServer(Message payload) { ClientPlayNetworking.send(payload.type(), encode(payload)); }
    private static net.minecraft.network.FriendlyByteBuf encode(Message payload) {
        var buffer = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        if (payload instanceof com.chaos.tablist.net.TabPayloads.SyncConfig p) com.chaos.tablist.net.TabPayloads.SyncConfig.CODEC.encode(buffer,p);
        else if (payload instanceof com.chaos.tablist.net.TabPayloads.SyncValues p) com.chaos.tablist.net.TabPayloads.SyncValues.CODEC.encode(buffer,p);
        else if (payload instanceof com.chaos.tablist.net.TabPayloads.SelfValues p) com.chaos.tablist.net.TabPayloads.SelfValues.CODEC.encode(buffer,p);
        else if (payload instanceof com.chaos.tablist.net.TabPayloads.OpenEditor p) com.chaos.tablist.net.TabPayloads.OpenEditor.CODEC.encode(buffer,p);
        else if (payload instanceof com.chaos.tablist.net.TabPayloads.IconData p) com.chaos.tablist.net.TabPayloads.IconData.CODEC.encode(buffer,p);
        else if (payload instanceof com.chaos.tablist.net.TabPayloads.SaveConfig p) com.chaos.tablist.net.TabPayloads.SaveConfig.CODEC.encode(buffer,p);
        else if (payload instanceof com.chaos.tablist.net.TabPayloads.SaveResult p) com.chaos.tablist.net.TabPayloads.SaveResult.CODEC.encode(buffer,p);
        else throw new IllegalArgumentException("Unknown Tab packet");
        return buffer;
    }
}
