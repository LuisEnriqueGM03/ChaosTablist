package com.chaos.tablist.forge;

import com.chaos.tablist.client.ChaosTablistClient;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

public final class ForgeClient {
    private ForgeClient() {}
    public static void register() {
        ChaosTablistClient.initialize();
        MinecraftForge.EVENT_BUS.addListener(ForgeClient::tick);
        MinecraftForge.EVENT_BUS.addListener(ForgeClient::logout);
    }
    public static void receive(CustomPacketPayload payload) { ChaosTablistClient.receive(payload); }
    private static void tick(TickEvent.ClientTickEvent.Post event) { ChaosTablistClient.tick(); }
    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ChaosTablistClient.disconnected(); }
}
