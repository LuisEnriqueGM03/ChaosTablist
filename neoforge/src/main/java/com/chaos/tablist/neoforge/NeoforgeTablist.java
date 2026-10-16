package com.chaos.tablist.neoforge;

import com.chaos.tablist.ChaosTablist;
import com.chaos.tablist.command.TabCommand;
import com.chaos.tablist.server.TabTicker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

@Mod(ChaosTablist.MOD_ID)
public final class NeoforgeTablist {
    public NeoforgeTablist() {
        NeoforgeNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        MinecraftForge.EVENT_BUS.addListener(this::started);
        MinecraftForge.EVENT_BUS.addListener(this::stopped);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::joined);
        MinecraftForge.EVENT_BUS.addListener(this::left);
        if (FMLEnvironment.dist == Dist.CLIENT) NeoforgeClient.register();
    }
    private void commands(RegisterCommandsEvent event) { TabCommand.register(event.getDispatcher()); }
    private void started(ServerStartedEvent event) { ChaosTablist.started(event.getServer()); }
    private void stopped(ServerStoppedEvent event) { ChaosTablist.stopped(); }
    private void tick(TickEvent.ServerTickEvent event) { if(event.phase == TickEvent.Phase.END) TabTicker.tick(event.getServer()); }
    private void joined(PlayerEvent.PlayerLoggedInEvent event) { if (event.getEntity() instanceof ServerPlayer player) TabTicker.join(player); }
    private void left(PlayerEvent.PlayerLoggedOutEvent event) { if (event.getEntity() instanceof ServerPlayer player) TabTicker.leave(player); }
}
