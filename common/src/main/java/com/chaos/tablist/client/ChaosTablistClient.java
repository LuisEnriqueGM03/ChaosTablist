package com.chaos.tablist.client;

import com.chaos.tablist.ChaosTablist;
import com.chaos.tablist.client.editor.TablistEditorScreen;
import com.chaos.tablist.config.TabConfig;
import com.chaos.tablist.net.TabPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

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
    public static void receive(CustomPacketPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        try {
            switch (payload) {
                case TabPayloads.SyncConfig p -> {
                    ClientState.setConfig(p.data(), p.revision());
                    if (mc.gui.screen() instanceof TablistEditorScreen editor)
                        editor.serverConfigChanged(ClientState.config(), ClientState.revision());
                }
                case TabPayloads.SyncValues p -> ClientState.setValues(p.data());
                case TabPayloads.SelfValues p -> ClientState.setSelfValues(p.data());
                case TabPayloads.IconData p -> CustomIcons.accept(p.name(), p.png());
                case TabPayloads.OpenEditor p -> mc.gui.setScreen(new TablistEditorScreen(
                    TabConfig.fromJson(TabPayloads.gunzip(p.data())), p.chaosRanks(), p.revision()));
                case TabPayloads.SaveResult p -> { if (mc.gui.screen() instanceof TablistEditorScreen editor) editor.saved(p); }
                default -> throw new IllegalArgumentException("Unexpected client payload " + payload.type().id());
            }
        } catch (RuntimeException e) { ChaosTablist.LOGGER.error("Invalid Tablist client payload", e); }
    }
}
