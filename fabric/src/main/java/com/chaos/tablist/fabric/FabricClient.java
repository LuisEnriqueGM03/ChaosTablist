package com.chaos.tablist.fabric;

import com.chaos.tablist.client.ChaosTablistClient;
import com.chaos.tablist.net.TabPayloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;

public final class FabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(TabPayloads.SyncConfig.TYPE, (client,handler,buf,sender) -> { var packet = TabPayloads.SyncConfig.CODEC.decode(buf); client.execute(() -> ChaosTablistClient.receive(packet)); });
        ClientPlayNetworking.registerGlobalReceiver(TabPayloads.SyncValues.TYPE, (client,handler,buf,sender) -> { var packet = TabPayloads.SyncValues.CODEC.decode(buf); client.execute(() -> ChaosTablistClient.receive(packet)); });
        ClientPlayNetworking.registerGlobalReceiver(TabPayloads.SelfValues.TYPE, (client,handler,buf,sender) -> { var packet = TabPayloads.SelfValues.CODEC.decode(buf); client.execute(() -> ChaosTablistClient.receive(packet)); });
        ClientPlayNetworking.registerGlobalReceiver(TabPayloads.IconData.TYPE, (client,handler,buf,sender) -> { var packet = TabPayloads.IconData.CODEC.decode(buf); client.execute(() -> ChaosTablistClient.receive(packet)); });
        ClientPlayNetworking.registerGlobalReceiver(TabPayloads.OpenEditor.TYPE, (client,handler,buf,sender) -> { var packet = TabPayloads.OpenEditor.CODEC.decode(buf); client.execute(() -> ChaosTablistClient.receive(packet)); });
        ClientPlayNetworking.registerGlobalReceiver(TabPayloads.SaveResult.TYPE, (client,handler,buf,sender) -> { var packet = TabPayloads.SaveResult.CODEC.decode(buf); client.execute(() -> ChaosTablistClient.receive(packet)); });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ChaosTablistClient::disconnected));
        ChaosTablistClient.initialize();
        ClientTickEvents.END_CLIENT_TICK.register(client -> ChaosTablistClient.tick());
    }
}
