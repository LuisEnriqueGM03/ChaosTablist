package com.chaos.tablist.neoforge;

import com.chaos.tablist.net.TabPayloads;
import com.chaos.tablist.server.TabServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Versioned optional payloads, handled on the game thread by NeoForge. */
public final class NeoforgeNetwork {
    private NeoforgeNetwork() {}
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2").optional();
        registrar.playToClient(TabPayloads.SyncConfig.TYPE, TabPayloads.SyncConfig.CODEC, (p,c) -> NeoforgeClient.receive(p));
        registrar.playToClient(TabPayloads.SyncValues.TYPE, TabPayloads.SyncValues.CODEC, (p,c) -> NeoforgeClient.receive(p));
        registrar.playToClient(TabPayloads.SelfValues.TYPE, TabPayloads.SelfValues.CODEC, (p,c) -> NeoforgeClient.receive(p));
        registrar.playToClient(TabPayloads.IconData.TYPE, TabPayloads.IconData.CODEC, (p,c) -> NeoforgeClient.receive(p));
        registrar.playToClient(TabPayloads.OpenEditor.TYPE, TabPayloads.OpenEditor.CODEC, (p,c) -> NeoforgeClient.receive(p));
        registrar.playToClient(TabPayloads.SaveResult.TYPE, TabPayloads.SaveResult.CODEC, (p,c) -> NeoforgeClient.receive(p));
        registrar.playToServer(TabPayloads.SaveConfig.TYPE, TabPayloads.SaveConfig.CODEC,
            (p,c) -> { if (c.player() instanceof ServerPlayer player) TabServer.save(player,p); });
    }
}
