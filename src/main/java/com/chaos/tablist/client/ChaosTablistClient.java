package com.chaos.tablist.client;

import com.chaos.tablist.client.editor.TablistEditorScreen;
import com.chaos.tablist.config.TabConfig;
import com.chaos.tablist.net.TabPayloads;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ChaosTablistClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(TabPayloads.SyncConfig.TYPE, (payload, context) ->
				context.client().execute(() -> ClientState.setConfig(payload.data())));
		ClientPlayNetworking.registerGlobalReceiver(TabPayloads.SyncValues.TYPE, (payload, context) ->
				context.client().execute(() -> ClientState.setValues(payload.data())));
		ClientPlayNetworking.registerGlobalReceiver(TabPayloads.IconData.TYPE, (payload, context) ->
				context.client().execute(() -> CustomIcons.accept(payload.name(), payload.png())));
		ClientPlayNetworking.registerGlobalReceiver(TabPayloads.OpenEditor.TYPE, (payload, context) ->
				context.client().execute(() -> {
					TabConfig config = TabConfig.fromJson(TabPayloads.gunzip(payload.data()));
					context.client().setScreen(new TablistEditorScreen(config, payload.chaosRanks()));
				}));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
			ClientState.reset();
			CustomIcons.clear();
		}));

		if (SelfTest.enabled()) {
			SelfTest.register();
		}
	}
}
