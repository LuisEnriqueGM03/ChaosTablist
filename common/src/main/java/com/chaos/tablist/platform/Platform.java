package com.chaos.tablist.platform;

import java.nio.file.Path;
import com.chaos.tablist.net.TabPayloads.Message;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Only loader-specific facilities belong here; configuration and rendering stay shared. */
public interface Platform {
    String name();
    Path configDirectory();
    boolean isModLoaded(String id);
    int modCount();
    boolean canSend(ServerPlayer player, ResourceLocation type);
    void send(ServerPlayer player, Message payload);
    void sendToServer(Message payload);
}
