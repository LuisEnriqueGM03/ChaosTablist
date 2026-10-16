package com.chaos.tablist.neoforge;

import com.chaos.tablist.client.ChaosTablistClient;
import com.chaos.tablist.net.TabPayloads.Message;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

public final class NeoforgeClient {
    private NeoforgeClient() {}
    public static void register() {
        ChaosTablistClient.initialize();
        MinecraftForge.EVENT_BUS.addListener(NeoforgeClient::tick);
        MinecraftForge.EVENT_BUS.addListener(NeoforgeClient::logout);
    }
    public static void receive(Message payload) { ChaosTablistClient.receive(payload); }
    private static void tick(TickEvent.ClientTickEvent event) { if(event.phase == TickEvent.Phase.END) ChaosTablistClient.tick(); }
    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ChaosTablistClient.disconnected(); }
}
