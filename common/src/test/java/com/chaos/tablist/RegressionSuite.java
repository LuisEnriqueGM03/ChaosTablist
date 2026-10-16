package com.chaos.tablist;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.chaos.tablist.client.editor.DesignLibrary;
import com.chaos.tablist.client.editor.EditorHistory;
import com.chaos.tablist.client.editor.EditorSaveState;
import com.chaos.tablist.config.ConfigFiles;
import com.chaos.tablist.config.PngLimits;
import com.chaos.tablist.config.TabConfig;
import com.chaos.tablist.config.TabStore;
import com.chaos.tablist.net.TabPayloads;
import com.chaos.tablist.server.ServerValues;
import com.chaos.tablist.text.ChaosText;
import com.chaos.tablist.text.Evaluator;
import com.chaos.tablist.text.TemplateDiagnostics;
import com.chaos.tablist.text.TextContext;
import com.chaos.tablist.text.VanillaValues;
import com.google.gson.JsonParser;

/** Regressions for persistence, privacy, network limits and editor state. No display or test world needed. */
public final class RegressionSuite {
    private static int checks;
    private static final TextContext CONTEXT = new TextContext() {
        public String value(String key) { return key.equals("long") ? "x".repeat(8000) : ""; }
        public double time() { return 1; }
        public boolean vanilla() { return true; }
        public String animationFrame(String name, double time) { return "{anim:loop}"; }
        public int pingColor(int ping) { return 0; }
        public net.minecraft.core.HolderLookup.Provider registries() { return null; }
    };

    private static void check(boolean result, String description) {
        checks++;
        if (!result) throw new AssertionError(description);
    }
    private static void rejects(Runnable action, String description) {
        try { action.run(); }
        catch (IllegalArgumentException | IllegalStateException expected) { checks++; return; }
        throw new AssertionError(description);
    }

    public static void main(String[] args) throws Exception {
        config(); text(); compression(); files(); privacy(); history(); saves(); png(); presets(); platform(); legacyWireLimits();
        System.out.println("Chaos Tablist: " + checks + " regression checks passed");
    }

    private static void platform() {
        String expected = System.getProperty("chaostablist.loader");
        String actual = com.chaos.tablist.platform.Services.PLATFORM.name();
        check(actual.equals(expected), "Matching loader service is packaged");
        boolean isolated = true;
        for (String loader : List.of("fabric", "forge", "neoforge")) {
            if (loader.equals(actual)) continue;
            String entry = loader.equals("neoforge") ? "NeoforgeTablist" : loader.equals("forge") ? "ForgeTablist" : "FabricTablist";
            isolated &= RegressionSuite.class.getClassLoader().getResource("com/chaos/tablist/" + loader + "/" + entry + ".class") == null;
        }
        check(isolated, "Other loaders' entrypoints are not on the runtime classpath");
        check(RegressionSuite.class.getResource("/assets/chaostablist/badge_meta.json") != null,
            "Shared resources are present for each loader");
    }

    private static void config() {
        TabConfig config = TabConfig.fromJson("{\"layout\":{\"openAnimation\":null,\"headerAlign\":null,\"objectiveStyle\":null,\"panelColor\":null},\"ping\":{\"style\":null,\"thresholds\":[null]}}");
        check(new TabConfig().header.get(1).contains("✦") && new TabConfig().footer.get(1).contains("▌"), "Default template symbols remain UTF-8");
        check(config.layout.openAnimation.equals("drop"), "Null animation gets a safe default");
        check(config.layout.headerAlign.equals("center"), "Null alignment gets a safe default");
        check(config.layout.objectiveStyle.equals("number"), "Null objective gets a safe default");
        check(config.layout.panelColor != null && config.ping.style.equals("both"), "Null colors and styles are safe");
        check(!config.ping.thresholds.isEmpty(), "Filtering null thresholds cannot leave an empty set");
        config.layout.scale = Float.NaN; config.layout.spectatorAlpha = Float.POSITIVE_INFINITY;
        config.sanitize();
        check(config.layout.scale == 1 && Float.isFinite(config.layout.spectatorAlpha), "Non-finite layout values are rejected");
        config.updateTicks = 0; config.layout.maxPlayers = Integer.MAX_VALUE; config.sanitize();
        check(config.updateTicks == 1 && config.layout.maxPlayers == 500, "Integer bounds are enforced");
        rejects(() -> TabConfig.fromJson("null"), "JSON null is not a valid configuration");
        rejects(() -> TabConfig.fromJson("[]"), "A configuration must be an object");
        rejects(() -> TabConfig.fromJson("{\"schemaVersion\":999}"), "Do not overwrite a future schema");
        config.animations.clear();
        for (int i = 0; i < 100; i++) config.animations.put("animation" + i, TabConfig.Animation.of(1, "frame"));
        config.sanitize();
        check(config.animations.size() == 64, "Animation count is bounded");
        check(config.animations.values().iterator().next().intervalMs == 50, "Animation interval is bounded");
        config.players.put("invalid-uuid", new TabConfig.PlayerOverride()); config.sanitize();
        check(config.players.isEmpty(), "Invalid player IDs do not reach the editor");
        check(TabConfig.Animation.of(1000, "one", "two").frame(-1).equals("two"), "Animation clocks may move backwards safely");
        check(config.copy().toJson().equals(config.toJson()), "Configuration round trip is stable");
    }

    private static void text() {
        check(Evaluator.eval("<scroll:2:-1>abcdef</scroll>", CONTEXT).size() == 2, "Negative marquee speed is safe");
        check(Evaluator.eval("a".repeat(3072), CONTEXT).size() == 2048, "Long text obeys the glyph budget");
        check(Evaluator.eval("<bar:50:100>".repeat(24), CONTEXT).size() == 2048, "Bars share the glyph budget");
        check(Evaluator.eval("{long}", CONTEXT).size() == 2048, "Placeholder expansion obeys the glyph budget");
        check(Evaluator.eval("{anim:loop}", CONTEXT).isEmpty(), "Recursive animations stop at the depth limit");
        rejects(() -> ChaosText.parse("<b>".repeat(33)), "Deep tag nesting is rejected");
        check(!ChaosText.parse("<b>".repeat(32) + "ok").isEmpty(), "Supported nesting is accepted");
        check(ChaosText.parse(null).isEmpty(), "Null parser input is safe");
        check(!TemplateDiagnostics.inspect("<unknown>hello").isEmpty(), "Unknown tag has a diagnostic");
        check(!TemplateDiagnostics.inspect("</b>").isEmpty(), "Unmatched closing tag has a diagnostic");
        check(!TemplateDiagnostics.inspect("{player").isEmpty(), "Unclosed placeholder has a diagnostic");
        check(TemplateDiagnostics.inspect("<gradient:#f00:#0f0>Hi</gradient>").isEmpty(), "Valid markup has no warning");
        Map<String, String> global = Map.of("time", "12:34", "date", "15/10/2026");
        check(VanillaValues.resolve("fps", Map.of(), Map.of(), global, "-").equals("-"), "Vanilla never resolves client FPS");
        check(VanillaValues.resolve("local_time_s", Map.of(), Map.of(), global, "-").equals("12:34"), "Clock fallback matches server");
        check(VanillaValues.resolve("my_ping", Map.of("ping", "999"), Map.of("ping", "30"), global, "-").equals("30"), "Viewer ping differs from row ping");
    }

    private static void compression() {
        String source = "áéíóú ✦ {player}";
        check(TabPayloads.gunzip(TabPayloads.gzip(source)).equals(source), "UTF-8 gzip round trip");
        rejects(() -> TabPayloads.gunzip(new byte[] {1, 2, 3}), "Malformed gzip is rejected");
        rejects(() -> TabPayloads.gunzip(TabPayloads.gzip(" ".repeat(ConfigFiles.MAX_BYTES + 1))), "Oversized gzip is rejected, not silently truncated");
        check(TabPayloads.gunzip(TabPayloads.gzip(" ".repeat(ConfigFiles.MAX_BYTES))).length() == ConfigFiles.MAX_BYTES, "Exact gzip boundary works");
        var original = new TabPayloads.SaveConfig(new byte[] {7, 8}, 123L, 456L);
        var buffer = io.netty.buffer.Unpooled.buffer();
        try {
            TabPayloads.SaveConfig.CODEC.encode(buffer, original);
            var decoded = TabPayloads.SaveConfig.CODEC.decode(buffer);
            check(decoded.revision() == 123 && decoded.requestId() == 456 && decoded.data()[1] == 8, "Save codec preserves revision and request ID");
        } finally { buffer.release(); }
    }

    private static void rejectsWire(Runnable action, String description) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError(description);
    }
    private static void legacyWireLimits() {
        var raw = io.netty.buffer.Unpooled.buffer();
        try {
            raw.writeBytes(new byte[] {(byte)0x81,(byte)0xFA,0x01});
            rejectsWire(() -> TabPayloads.SaveConfig.CODEC.decode(raw), "Legacy C2S rejects declared oversized arrays before allocation");
            raw.clear(); raw.writeBytes(new byte[] {(byte)0xC1,(byte)0x84,0x3D});
            rejectsWire(() -> TabPayloads.SyncConfig.CODEC.decode(raw), "Legacy S2C rejects declared oversized arrays before allocation");
            raw.clear();
            rejectsWire(() -> TabPayloads.IconData.CODEC.encode(raw, new TabPayloads.IconData("a".repeat(65),new byte[0])), "Legacy icon names retain the UTF-8 length cap");
        } finally { raw.release(); }
    }

    private static void files() throws Exception {
        Path dir = Files.createTempDirectory("chaostablist-regression-");
        Path file = dir.resolve("tablist.json");
        ConfigFiles.write(file, "first"); ConfigFiles.write(file, "second");
        check(ConfigFiles.read(file).equals("second"), "Replace-on-success writes new content");
        check(Files.readString(dir.resolve("tablist.json.bak")).equals("first"), "Previous file is backed up");
        Files.writeString(file, " ".repeat(ConfigFiles.MAX_BYTES + 1));
        try { ConfigFiles.read(file); throw new AssertionError("Oversized configuration was accepted"); }
        catch (java.io.IOException expected) { checks++; }
        Path world = dir.resolve("world");
        check(TabStore.loadDirectory(world), "Missing configuration creates defaults");
        TabConfig good = TabStore.config().copy(); good.serverName = "Before error";
        check(TabStore.set(good), "Valid configuration persists");
        long revision = TabStore.revision();
        Files.writeString(world.resolve("tablist.json"), "{broken");
        check(!TabStore.loadDirectory(world), "Invalid reload reports failure");
        check(TabStore.config().serverName.equals("Before error"), "Invalid reload keeps live state");
        check(Files.readString(world.resolve("tablist.json")).equals("{broken"), "Invalid file stays intact");
        check(TabStore.revision() == revision, "Failed reload does not change revision");
        check(TabStore.set(good), "Explicit save can recover the invalid configuration");
        check(Files.readString(world.resolve("tablist.json.bak")).equals("{broken"), "Invalid manual edits are recoverable from backup");
        Path blocker = dir.resolve("not-a-directory"); Files.writeString(blocker, "keep");
        TabStore.loadDirectory(blocker.resolve("child"));
        check(!TabStore.set(good), "Disk failure is reported instead of acknowledged");
        check(Files.readString(blocker).equals("keep"), "Disk failure preserves existing file");
    }

    private static void privacy() throws Exception {
        UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
        Map<UUID, Map<String, String>> players = new HashMap<>();
        players.put(alice, Map.of("player", "Alice", "uuid", alice.toString(), "x", "123", "health", "20", "team", "red"));
        players.put(bob, Map.of("player", "Bob", "uuid", bob.toString(), "x", "999", "health", "4", "team", "blue"));
        var field = ServerValues.class.getDeclaredField("players"); field.setAccessible(true); field.set(null, players);
        TabConfig config = new TabConfig();
        var configField = ServerValues.class.getDeclaredField("lastConfig"); configField.setAccessible(true); configField.set(null, config);
        var publicData = JsonParser.parseString(ServerValues.toJson()).getAsJsonObject().getAsJsonObject("p");
        check(!publicData.getAsJsonObject(alice.toString()).has("x"), "Coordinates are excluded from public packets");
        check(!publicData.getAsJsonObject(bob.toString()).has("health"), "Health is private by default");
        check(publicData.getAsJsonObject(alice.toString()).get("team").getAsString().equals("red"), "Public team data is available for sorting");
        var own = JsonParser.parseString(ServerValues.selfJson(alice)).getAsJsonObject();
        check(own.get("x").getAsString().equals("123") && !own.toString().contains("999"), "Personal packet contains only its owner's data");
        config.publicPlayerValues.add("health");
        publicData = JsonParser.parseString(ServerValues.toJson()).getAsJsonObject().getAsJsonObject("p");
        check(publicData.getAsJsonObject(bob.toString()).has("health"), "Administrators can explicitly publish selected fields");
        check(!publicData.getAsJsonObject(bob.toString()).has("x"), "Publishing health does not publish coordinates");
        ServerValues.clear();
    }

    private static void history() {
        EditorHistory history = new EditorHistory("original");
        history.changed(0); history.changed(100);
        check(history.undo("typed", false).equals("original"), "Continuous typing is one undo step");
        check(history.undo("original", true).equals("typed"), "Redo restores the draft");
        history.changed(200); history.settle("new edit");
        check(history.undo("new edit", true) == null, "New edits invalidate redo");
        history.checkpoint("new edit"); history.settle("defaults");
        check(history.undo("defaults", false).equals("new edit"), "Reset/import can be undone");
    }

    private static void saves() {
        EditorSaveState state = new EditorSaveState(4, "old");
        var pending = state.begin("draft", 100);
        check(state.savedJson().equals("old"), "Sending does not mark a draft saved");
        check(!state.accept(new TabPayloads.SaveResult(pending.id() + 1, true, "saved", 5)), "Stale acknowledgement is ignored");
        state.accept(new TabPayloads.SaveResult(pending.id(), false, "disk", 4));
        check(state.savedJson().equals("old") && state.error().equals("disk"), "Disk rejection retains the original baseline");
        pending = state.begin("draft", 200);
        state.accept(new TabPayloads.SaveResult(pending.id(), true, "saved", 5));
        check(state.savedJson().equals("draft") && state.revision() == 5, "Only a positive acknowledgement advances the baseline");
        pending = state.begin("another", 300);
        state.accept(new TabPayloads.SaveResult(pending.id(), false, "conflict", 6));
        check(state.revision() == 5 && state.error().equals("conflict"), "A conflict cannot silently advance the revision");
        state.begin("another", 400);
        check(!state.timedOut(15399) && state.timedOut(15400), "Missing acknowledgements time out predictably");
        check(state.savedJson().equals("draft"), "Timeout retains the last confirmed baseline");
    }

    private static void png() {
        byte[] png = new byte[33];
        ByteBuffer buffer = ByteBuffer.wrap(png);
        buffer.put(new byte[] {(byte)137, 80, 78, 71, 13, 10, 26, 10});
        buffer.putInt(13); buffer.putInt(0x49484452); buffer.putInt(1024); buffer.putInt(1024);
        PngLimits.validate(png); checks++;
        buffer.putInt(16, Integer.MAX_VALUE);
        rejects(() -> PngLimits.validate(png), "Huge decoded dimensions are rejected before allocation");
        rejects(() -> PngLimits.validate(new byte[40]), "Non-PNG data is rejected");
        check(PngLimits.validName("logo-1") && !PngLimits.validName("../logo") && !PngLimits.validName("a".repeat(65)), "Icon paths and lengths are bounded");
    }

    private static void presets() {
        TabConfig original = new TabConfig(); original.serverName = "My server";
        original.publicPlayerValues.add("health");
        for (String name : List.of("chaos", "minimal", "ocean")) {
            TabConfig preset = DesignLibrary.preset(name, original);
            check(preset.serverName.equals("My server"), "Preset retains server identity: " + name);
            check(preset.publicPlayerValues.equals(List.of("health")), "Preset retains explicit privacy choices: " + name);
            check(preset.copy() != null, "Preset passes full validation: " + name);
        }
    }
}
