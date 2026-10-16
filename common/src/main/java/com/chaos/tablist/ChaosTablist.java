package com.chaos.tablist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.chaos.tablist.config.TabStore;
import com.chaos.tablist.server.ServerValues;
import com.chaos.tablist.server.TabServer;
import com.chaos.tablist.server.TabSorting;
import com.chaos.tablist.server.TabTicker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/** Shared lifecycle; each loader supplies its native events and networking. */
public final class ChaosTablist {
    public static final String MOD_ID = "chaostablist";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private ChaosTablist() {}
    public static ResourceLocation id(String path) { return new ResourceLocation(MOD_ID, path); }
    public static void started(MinecraftServer server) {
        ServerValues.markStarted(); TabStore.load(server); TabSorting.clear(server);
    }
    public static void stopped() { TabTicker.stop(); TabServer.clear(); ServerValues.clear(); }
}
