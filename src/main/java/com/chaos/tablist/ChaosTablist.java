package com.chaos.tablist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chaos.tablist.command.TabCommand;
import com.chaos.tablist.config.TabStore;
import com.chaos.tablist.net.TabPayloads;
import com.chaos.tablist.server.ServerValues;
import com.chaos.tablist.server.TabServer;
import com.chaos.tablist.server.TabTicker;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.ResourceLocation;

public class ChaosTablist implements ModInitializer {

	public static final String MOD_ID = "chaostablist";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playS2C().register(TabPayloads.SyncConfig.TYPE, TabPayloads.SyncConfig.CODEC);
		PayloadTypeRegistry.playS2C().register(TabPayloads.SyncValues.TYPE, TabPayloads.SyncValues.CODEC);
		PayloadTypeRegistry.playS2C().register(TabPayloads.OpenEditor.TYPE, TabPayloads.OpenEditor.CODEC);
		PayloadTypeRegistry.playS2C().register(TabPayloads.IconData.TYPE, TabPayloads.IconData.CODEC);
		PayloadTypeRegistry.playC2S().register(TabPayloads.SaveConfig.TYPE, TabPayloads.SaveConfig.CODEC);
		TabServer.register();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				TabCommand.register(dispatcher));

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			ServerValues.markStarted();
			TabStore.load(server);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> TabTicker.stop());
		ServerTickEvents.END_SERVER_TICK.register(TabTicker::tick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> TabTicker.join(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> TabTicker.leave(handler.player));
	}
}
