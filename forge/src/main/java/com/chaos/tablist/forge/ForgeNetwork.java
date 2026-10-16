package com.chaos.tablist.forge;

import com.chaos.tablist.ChaosTablist;
import com.chaos.tablist.net.TabPayloads;
import com.chaos.tablist.server.TabServer;
import net.minecraftforge.network.*;

/** Optional, versioned channel: vanilla peers remain supported. All handlers run on the game thread. */
public final class ForgeNetwork {
    public static final SimpleChannel CHANNEL = ChannelBuilder.named(ChaosTablist.id("tablist_v2"))
        .networkProtocolVersion(2).optional().simpleChannel();
    private ForgeNetwork() {}
    public static void register() {
        CHANNEL.messageBuilder(TabPayloads.SyncConfig.class, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((p,b) -> TabPayloads.SyncConfig.CODEC.encode(b,p)).decoder(TabPayloads.SyncConfig.CODEC::decode)
            .consumerMainThread((p,c) -> ForgeClient.receive(p)).add();
        CHANNEL.messageBuilder(TabPayloads.SyncValues.class, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((p,b) -> TabPayloads.SyncValues.CODEC.encode(b,p)).decoder(TabPayloads.SyncValues.CODEC::decode)
            .consumerMainThread((p,c) -> ForgeClient.receive(p)).add();
        CHANNEL.messageBuilder(TabPayloads.SelfValues.class, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((p,b) -> TabPayloads.SelfValues.CODEC.encode(b,p)).decoder(TabPayloads.SelfValues.CODEC::decode)
            .consumerMainThread((p,c) -> ForgeClient.receive(p)).add();
        CHANNEL.messageBuilder(TabPayloads.IconData.class, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((p,b) -> TabPayloads.IconData.CODEC.encode(b,p)).decoder(TabPayloads.IconData.CODEC::decode)
            .consumerMainThread((p,c) -> ForgeClient.receive(p)).add();
        CHANNEL.messageBuilder(TabPayloads.OpenEditor.class, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((p,b) -> TabPayloads.OpenEditor.CODEC.encode(b,p)).decoder(TabPayloads.OpenEditor.CODEC::decode)
            .consumerMainThread((p,c) -> ForgeClient.receive(p)).add();
        CHANNEL.messageBuilder(TabPayloads.SaveResult.class, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((p,b) -> TabPayloads.SaveResult.CODEC.encode(b,p)).decoder(TabPayloads.SaveResult.CODEC::decode)
            .consumerMainThread((p,c) -> ForgeClient.receive(p)).add();
        CHANNEL.messageBuilder(TabPayloads.SaveConfig.class, NetworkDirection.PLAY_TO_SERVER)
            .encoder((p,b) -> TabPayloads.SaveConfig.CODEC.encode(b,p)).decoder(TabPayloads.SaveConfig.CODEC::decode)
            .consumerMainThread((p,c) -> { if (c.getSender() != null) TabServer.save(c.getSender(),p); }).add();
        CHANNEL.build();
    }
}
