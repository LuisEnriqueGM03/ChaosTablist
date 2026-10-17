package com.chaos.tablist.forge;

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
public final class ForgeTablist {
    public ForgeTablist() {
        ForgeNetwork.register();
        RegisterCommandsEvent.BUS.addListener(this::commands);
        ServerStartedEvent.BUS.addListener(this::started);
        ServerStoppedEvent.BUS.addListener(this::stopped);
        TickEvent.ServerTickEvent.Post.BUS.addListener(this::tick);
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(this::joined);
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(this::left);
        if (FMLEnvironment.dist == Dist.CLIENT) ForgeClient.register();
    }
    private void commands(RegisterCommandsEvent event) { TabCommand.register(event.getDispatcher()); }
    private void started(ServerStartedEvent event) { ChaosTablist.started(event.getServer()); }
    private void stopped(ServerStoppedEvent event) { ChaosTablist.stopped(); }
    private void tick(TickEvent.ServerTickEvent.Post event) { TabTicker.tick(event.server()); }
    private void joined(PlayerEvent.PlayerLoggedInEvent event) { if (event.getEntity() instanceof ServerPlayer player) TabTicker.join(player); }
    private void left(PlayerEvent.PlayerLoggedOutEvent event) { if (event.getEntity() instanceof ServerPlayer player) TabTicker.leave(player); }
}
