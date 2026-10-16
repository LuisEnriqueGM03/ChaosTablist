package com.chaos.tablist.client.editor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.chaos.tablist.config.ConfigFiles;
import com.chaos.tablist.config.TabConfig;

/** Local reusable designs. Applying a preset preserves the server identity and player overrides. */
public final class DesignLibrary {
    private DesignLibrary() {}
    public static TabConfig preset(String name, TabConfig current) {
        TabConfig out = current.copy();
        TabConfig defaults = new TabConfig();
        out.header = defaults.header; out.footer = defaults.footer; out.layout = defaults.layout;
        out.ping = defaults.ping; out.animations = defaults.animations; out.row = defaults.row;
        switch (name) {
            case "minimal" -> {
                out.header = new java.util.ArrayList<>(List.of("<b><white>{server_name}", "<gray>{online}/{max_players}"));
                out.footer = new java.util.ArrayList<>(List.of("<gray>TPS <white>{tps} <dark_gray>| <pc:{ping}>{ping}ms"));
                out.layout.panelColor = "#CC101010"; out.layout.panelColor2 = "#CC101010";
                out.layout.borderWidth = 0; out.layout.animatedBorder = false; out.layout.openAnimation = "fade";
                out.layout.separatorColor = "#00000000"; out.ping.style = "number";
            }
            case "ocean" -> {
                out.layout.panelColor = "#D0051626"; out.layout.panelColor2 = "#D00B3040";
                out.layout.borderColor = "#FF00B8D9"; out.layout.borderColor2 = "#FF80FFDB";
                out.layout.selfRowColor = "#5500B8D9";
                out.header = new java.util.ArrayList<>(List.of("<b><gradient:#80FFDB:#00B8D9>{server_name}</gradient>",
                    "<gray>{online}/{max_players} <dark_gray>| <pc:{ping}>{ping}ms"));
                out.footer = new java.util.ArrayList<>(List.of("<gray>TPS <white>{tps} <dark_gray>| <gray>{local_time}"));
            }
            case "chaos" -> {}
            default -> throw new IllegalArgumentException("Unknown design");
        }
        out.sanitize();
        return out;
    }

    public static Path directory() {
        return com.chaos.tablist.platform.Services.PLATFORM.configDirectory().resolve("chaostablist/designs");
    }
    public static Path export(TabConfig config) throws IOException {
        Path dir = directory(); Files.createDirectories(dir);
        String prefix = "design-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-";
        Path file = Files.createTempFile(dir, prefix, ".json");
        try { ConfigFiles.write(file, config.copy().toJson()); }
        catch (IOException | RuntimeException e) { Files.deleteIfExists(file); throw e; }
        // The newly created empty file has no previous design to preserve.
        Files.deleteIfExists(file.resolveSibling(file.getFileName() + ".bak"));
        return file;
    }
    public static List<Path> files() throws IOException {
        Path dir = directory(); Files.createDirectories(dir);
        try (var files = Files.list(dir)) {
            return files.filter(Files::isRegularFile).filter(p -> p.getFileName().toString().endsWith(".json"))
                .sorted(java.util.Comparator.reverseOrder()).limit(100).toList();
        }
    }
    public static TabConfig read(Path file) throws IOException { return TabConfig.fromJson(ConfigFiles.read(file)); }
}
