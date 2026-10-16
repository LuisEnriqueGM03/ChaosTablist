package com.chaos.tablist.neoforge;

import com.chaos.tablist.ChaosTablist;
import com.chaos.tablist.command.TabCommand;
import com.chaos.tablist.server.TabTicker;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@Mod(ChaosTablist.MOD_ID)
public final class NeoforgeTablist {
    public NeoforgeTablist(IEventBus bus) {
        bus.addListener(NeoforgeNetwork::register);
        NeoForge.EVENT_BUS.addListener(this::commands);
        NeoForge.EVENT_BUS.addListener(this::started);
        NeoForge.EVENT_BUS.addListener(this::stopped);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::joined);
        NeoForge.EVENT_BUS.addListener(this::left);
        if (FMLEnvironment.dist == Dist.CLIENT) NeoforgeClient.register();
    }
    private void commands(RegisterCommandsEvent event) { TabCommand.register(event.getDispatcher()); }
    private void started(ServerStartedEvent event) { ChaosTablist.started(event.getServer()); }
    private void stopped(ServerStoppedEvent event) { ChaosTablist.stopped(); }
    private void tick(ServerTickEvent.Post event) { TabTicker.tick(event.getServer()); }
    private void joined(PlayerEvent.PlayerLoggedInEvent event) { if (event.getEntity() instanceof ServerPlayer player) TabTicker.join(player); }
    private void left(PlayerEvent.PlayerLoggedOutEvent event) { if (event.getEntity() instanceof ServerPlayer player) TabTicker.leave(player); }
}
