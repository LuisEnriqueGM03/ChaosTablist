package com.chaos.tablist.fabric;

import com.chaos.tablist.ChaosTablist;
import com.chaos.tablist.command.TabCommand;
import com.chaos.tablist.net.TabPayloads;
import com.chaos.tablist.server.TabServer;
import com.chaos.tablist.server.TabTicker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;

public final class FabricTablist implements ModInitializer {
    @Override public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(TabPayloads.SyncConfig.TYPE, TabPayloads.SyncConfig.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TabPayloads.SyncValues.TYPE, TabPayloads.SyncValues.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TabPayloads.SelfValues.TYPE, TabPayloads.SelfValues.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TabPayloads.IconData.TYPE, TabPayloads.IconData.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TabPayloads.OpenEditor.TYPE, TabPayloads.OpenEditor.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TabPayloads.SaveResult.TYPE, TabPayloads.SaveResult.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TabPayloads.SaveConfig.TYPE, TabPayloads.SaveConfig.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TabPayloads.SaveConfig.TYPE, (p, c) -> c.server().execute(() -> TabServer.save(c.player(), p)));
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> TabCommand.register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(ChaosTablist::started);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ChaosTablist.stopped());
        ServerTickEvents.END_SERVER_TICK.register(TabTicker::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> TabTicker.join(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> TabTicker.leave(handler.player));
    }
}
