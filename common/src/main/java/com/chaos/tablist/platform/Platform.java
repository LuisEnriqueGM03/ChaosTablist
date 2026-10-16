package com.chaos.tablist.platform;

import java.nio.file.Path;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Only loader-specific facilities belong here; configuration and rendering stay shared. */
public interface Platform {
    String name();
    Path configDirectory();
    boolean isModLoaded(String id);
    int modCount();
    boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type);
    void send(ServerPlayer player, CustomPacketPayload payload);
    void sendToServer(CustomPacketPayload payload);
}
