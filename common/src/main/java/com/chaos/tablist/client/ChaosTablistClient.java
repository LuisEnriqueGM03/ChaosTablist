package com.chaos.tablist.client;

import com.chaos.tablist.ChaosTablist;
import com.chaos.tablist.client.editor.TablistEditorScreen;
import com.chaos.tablist.config.TabConfig;
import com.chaos.tablist.net.TabPayloads;
import net.minecraft.client.Minecraft;
import com.chaos.tablist.net.TabPayloads.Message;
import net.minecraft.resources.ResourceLocation;

/** Shared client handlers. Loader adapters call receive on the client thread. */
public final class ChaosTablistClient {
    private ChaosTablistClient() {}
    public static void initialize() { if (Showcase.enabled()) Showcase.initialize(); }
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (SelfTest.enabled()) SelfTest.tick(mc);
        if (Showcase.enabled()) Showcase.tick(mc);
    }
    public static void disconnected() { ClientState.reset(); CustomIcons.clear(); }
    public static void receive(Message payload) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (payload instanceof TabPayloads.SyncConfig p) {
                ClientState.setConfig(p.data(), p.revision());
                if (mc.screen instanceof TablistEditorScreen editor) editor.serverConfigChanged(ClientState.config(), ClientState.revision());
            } else if (payload instanceof TabPayloads.SyncValues p) ClientState.setValues(p.data());
            else if (payload instanceof TabPayloads.SelfValues p) ClientState.setSelfValues(p.data());
            else if (payload instanceof TabPayloads.IconData p) CustomIcons.accept(p.name(),p.png());
            else if (payload instanceof TabPayloads.OpenEditor p) mc.setScreen(new TablistEditorScreen(TabConfig.fromJson(TabPayloads.gunzip(p.data())),p.chaosRanks(),p.revision()));
            else if (payload instanceof TabPayloads.SaveResult p) { if(mc.screen instanceof TablistEditorScreen editor) editor.saved(p); }
            else throw new IllegalArgumentException("Unknown payload: " + payload.type());
        } catch (RuntimeException e) { ChaosTablist.LOGGER.error("Invalid Tablist client payload", e); }
    }
}
