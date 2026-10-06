package com.chaos.tablist.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

	public static final int MAX_ICON_BYTES = 256 * 1024;
	public static final int MAX_ICONS = 64;

	private static TabConfig config = new TabConfig();
	private static Map<String, byte[]> icons = new LinkedHashMap<>();
	private static Path dir;

	private TabStore() {}

	public static TabConfig config() {
		return config;
	}

	public static Map<String, byte[]> icons() {
		return icons;
	}

	public static void load(MinecraftServer server) {
		dir = server.getWorldPath(LevelResource.ROOT).resolve(ChaosTablist.MOD_ID);
		Path file = dir.resolve("tablist.json");
		TabConfig loaded = null;
		if (Files.exists(file)) {
			try {
				loaded = TabConfig.fromJson(Files.readString(file, StandardCharsets.UTF_8));
			} catch (Exception e) {
				ChaosTablist.LOGGER.error("No se pudo leer {}; se usa la configuración por defecto", file, e);
			}
		}
		if (loaded == null) {
			loaded = new TabConfig();
			loaded.sanitize();
			config = loaded;
			save();
		}
		config = loaded;
		loadIcons();
		ChaosTablist.LOGGER.info("Tab cargado: {} animaciones, {} iconos propios", config.animations.size(), icons.size());
	}

	public static void set(TabConfig newConfig) {
		newConfig.sanitize();
		config = newConfig;
		save();
	}

	public static void save() {
		if (dir == null) {
			return;
		}
		try {
			Files.createDirectories(dir.resolve("icons"));
			Files.writeString(dir.resolve("tablist.json"), config.toJson(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			ChaosTablist.LOGGER.error("No se pudo guardar tablist.json", e);
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
					byte[] data = Files.readAllBytes(p);
					if (data.length > MAX_ICON_BYTES) {
						ChaosTablist.LOGGER.warn("Icono {} demasiado grande ({} KB, máximo {} KB)", p, data.length / 1024,
								MAX_ICON_BYTES / 1024);
						continue;
					}
					found.put(name, data);
				}
			} catch (IOException e) {
				ChaosTablist.LOGGER.error("No se pudieron leer los iconos de {}", iconDir, e);
			}
		}
		icons = found;
	}
}
