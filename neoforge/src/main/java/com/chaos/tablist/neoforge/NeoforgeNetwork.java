package com.chaos.tablist.neoforge;

import com.chaos.tablist.ChaosTablist;
import com.chaos.tablist.net.TabPayloads;
import com.chaos.tablist.server.TabServer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

/** Optional, versioned channel: vanilla peers remain supported. All handlers run on the game thread. */
public final class NeoforgeNetwork {
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(ChaosTablist.id("tablist_v2"))
        .networkProtocolVersion(() -> "2")
        .clientAcceptedVersions(v -> v.equals("2") || v.equals(NetworkRegistry.ABSENT) || v.equals(NetworkRegistry.ACCEPTVANILLA))
        .serverAcceptedVersions(v -> v.equals("2") || v.equals(NetworkRegistry.ABSENT) || v.equals(NetworkRegistry.ACCEPTVANILLA)).simpleChannel();
    private NeoforgeNetwork() {}
    public static void register() {
        CHANNEL.messageBuilder(TabPayloads.SyncConfig.class, 0, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((p,b) -> TabPayloads.SyncConfig.CODEC.encode(b,p)).decoder(TabPayloads.SyncConfig.CODEC::decode)
            .consumerMainThread((p,c) -> NeoforgeClient.receive(p)).add();
        CHANNEL.messageBuilder(TabPayloads.SyncValues.class, 1, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((p,b) -> TabPayloads.SyncValues.CODEC.encode(b,p)).decoder(TabPayloads.SyncValues.CODEC::decode)
            .consumerMainThread((p,c) -> NeoforgeClient.receive(p)).add();
        CHANNEL.messageBuilder(TabPayloads.SelfValues.class, 2, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((p,b) -> TabPayloads.SelfValues.CODEC.encode(b,p)).decoder(TabPayloads.SelfValues.CODEC::decode)
            .consumerMainThread((p,c) -> NeoforgeClient.receive(p)).add();
        CHANNEL.messageBuilder(TabPayloads.IconData.class, 4, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((p,b) -> TabPayloads.IconData.CODEC.encode(b,p)).decoder(TabPayloads.IconData.CODEC::decode)
            .consumerMainThread((p,c) -> NeoforgeClient.receive(p)).add();
        CHANNEL.messageBuilder(TabPayloads.OpenEditor.class, 3, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((p,b) -> TabPayloads.OpenEditor.CODEC.encode(b,p)).decoder(TabPayloads.OpenEditor.CODEC::decode)
            .consumerMainThread((p,c) -> NeoforgeClient.receive(p)).add();
        CHANNEL.messageBuilder(TabPayloads.SaveResult.class, 6, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((p,b) -> TabPayloads.SaveResult.CODEC.encode(b,p)).decoder(TabPayloads.SaveResult.CODEC::decode)
            .consumerMainThread((p,c) -> NeoforgeClient.receive(p)).add();
        CHANNEL.messageBuilder(TabPayloads.SaveConfig.class, 5, NetworkDirection.PLAY_TO_SERVER)
            .encoder((p,b) -> TabPayloads.SaveConfig.CODEC.encode(b,p)).decoder(TabPayloads.SaveConfig.CODEC::decode)
            .consumerMainThread((p,c) -> { if (c.get().getSender() != null) TabServer.save(c.get().getSender(),p); }).add();
    }
}
