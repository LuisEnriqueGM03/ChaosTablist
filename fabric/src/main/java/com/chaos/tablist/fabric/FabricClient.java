package com.chaos.tablist.fabric;

import com.chaos.tablist.client.ChaosTablistClient;
import com.chaos.tablist.net.TabPayloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;

public final class FabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(TabPayloads.SyncConfig.TYPE, (p,c) -> c.client().execute(() -> ChaosTablistClient.receive(p)));
        ClientPlayNetworking.registerGlobalReceiver(TabPayloads.SyncValues.TYPE, (p,c) -> c.client().execute(() -> ChaosTablistClient.receive(p)));
        ClientPlayNetworking.registerGlobalReceiver(TabPayloads.SelfValues.TYPE, (p,c) -> c.client().execute(() -> ChaosTablistClient.receive(p)));
        ClientPlayNetworking.registerGlobalReceiver(TabPayloads.IconData.TYPE, (p,c) -> c.client().execute(() -> ChaosTablistClient.receive(p)));
        ClientPlayNetworking.registerGlobalReceiver(TabPayloads.OpenEditor.TYPE, (p,c) -> c.client().execute(() -> ChaosTablistClient.receive(p)));
        ClientPlayNetworking.registerGlobalReceiver(TabPayloads.SaveResult.TYPE, (p,c) -> c.client().execute(() -> ChaosTablistClient.receive(p)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ChaosTablistClient::disconnected));
        ChaosTablistClient.initialize();
        ClientTickEvents.END_CLIENT_TICK.register(client -> ChaosTablistClient.tick());
    }
}
