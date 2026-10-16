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
        ServerPlayNetworking.registerGlobalReceiver(TabPayloads.SaveConfig.TYPE, (server,player,handler,buf,sender) -> { var packet = TabPayloads.SaveConfig.CODEC.decode(buf); server.execute(() -> TabServer.save(player,packet)); });
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> TabCommand.register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(ChaosTablist::started);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ChaosTablist.stopped());
        ServerTickEvents.END_SERVER_TICK.register(TabTicker::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> TabTicker.join(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> TabTicker.leave(handler.player));
    }
}
