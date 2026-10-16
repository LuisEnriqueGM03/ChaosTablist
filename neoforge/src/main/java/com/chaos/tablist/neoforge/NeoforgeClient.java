package com.chaos.tablist.neoforge;

import com.chaos.tablist.client.ChaosTablistClient;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

public final class NeoforgeClient {
    private NeoforgeClient() {}
    public static void register() {
        ChaosTablistClient.initialize();
        NeoForge.EVENT_BUS.addListener(NeoforgeClient::tick);
        NeoForge.EVENT_BUS.addListener(NeoforgeClient::logout);
    }
    public static void receive(CustomPacketPayload payload) { ChaosTablistClient.receive(payload); }
    private static void tick(ClientTickEvent.Post event) { ChaosTablistClient.tick(); }
    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ChaosTablistClient.disconnected(); }
}
