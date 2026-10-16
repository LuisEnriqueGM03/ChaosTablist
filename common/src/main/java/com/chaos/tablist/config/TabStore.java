package com.chaos.tablist.config;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.chaos.tablist.ChaosTablist;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * La configuración del Tab y los iconos propios, en &lt;mundo&gt;/chaostablist/:
 * tablist.json y icons/*.png (se mandan a los clientes con el mod y se usan con &lt;icon:custom/nombre&gt;).
 */
public final class TabStore {

	public static final int MAX_ICON_BYTES = PngLimits.MAX_BYTES;
	public static final int MAX_ICONS = PngLimits.MAX_ICONS;

	private static TabConfig config = new TabConfig();
	private static Map<String, byte[]> icons = new LinkedHashMap<>();
	private static Path dir;
	private static long revision;

	public static long revision() { return revision; }

	private TabStore() {}

	public static TabConfig config() {
		return config;
	}

	public static Map<String, byte[]> icons() {
		return icons;
	}

	public static boolean load(MinecraftServer server) {
        return loadDirectory(server.getWorldPath(LevelResource.ROOT).resolve(ChaosTablist.MOD_ID));
    }

    public static boolean loadDirectory(Path nextDir) {
        boolean sameWorld = nextDir.equals(dir);
        dir = nextDir;
        Path file = dir.resolve("tablist.json");
        if (!sameWorld) config = new TabConfig();
        try {
            TabConfig loaded;
            if (Files.exists(file)) {
                loaded = TabConfig.fromJson(ConfigFiles.read(file));
            } else {
                loaded = new TabConfig();
                loaded.sanitize();
                ConfigFiles.write(file, loaded.toJson());
            }
            Files.createDirectories(dir.resolve("icons"));
            config = loaded;
            revision++;
            loadIcons();
            ChaosTablist.LOGGER.info("Tab cargado: {} animaciones, {} iconos propios", config.animations.size(), icons.size());
            return true;
        } catch (Exception e) {
            // Never replace an unreadable file with defaults. A reload keeps the live configuration.
            ChaosTablist.LOGGER.error("No se pudo leer {}; se conserva el archivo y la configuración anterior", file, e);
            loadIcons();
            return false;
        }
    }

    public static boolean set(TabConfig newConfig) {
        try {
            newConfig.sanitize();
            com.chaos.tablist.text.TemplateDiagnostics.validate(newConfig);
            if (dir == null) throw new IOException("No world is loaded");
            ConfigFiles.write(dir.resolve("tablist.json"), newConfig.toJson());
            config = newConfig;
            revision++;
            return true;
        } catch (Exception e) {
            ChaosTablist.LOGGER.error("No se pudo guardar tablist.json; se conserva la configuración anterior", e);
            return false;
        }
    }

	private static void loadIcons() {
		Map<String, byte[]> found = new LinkedHashMap<>();
		Path iconDir = dir.resolve("icons");
		if (Files.isDirectory(iconDir)) {
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(iconDir, "*.png")) {
				for (Path p : stream) {
					if (found.size() >= MAX_ICONS) {
						break;
					}
					String name = p.getFileName().toString();
					name = name.substring(0, name.length() - 4).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
					if (!PngLimits.validName(name) || found.containsKey(name) || !Files.isRegularFile(p)) continue;
					if (Files.size(p) > MAX_ICON_BYTES) {
						ChaosTablist.LOGGER.warn("Icono {} demasiado grande ({} KB, máximo {} KB)", p, Files.size(p) / 1024,
								MAX_ICON_BYTES / 1024);
						continue;
					}
					try (var in = Files.newInputStream(p)) {
                        byte[] data = in.readNBytes(MAX_ICON_BYTES + 1);
                        PngLimits.validate(data);
                        found.put(name, data);
                    } catch (IllegalArgumentException e) {
                        ChaosTablist.LOGGER.warn("Icono inválido {}: {}", p, e.getMessage());
                    }
				}
			} catch (IOException e) {
				ChaosTablist.LOGGER.error("No se pudieron leer los iconos de {}", iconDir, e);
			}
		}
		icons = found;
	}
}
