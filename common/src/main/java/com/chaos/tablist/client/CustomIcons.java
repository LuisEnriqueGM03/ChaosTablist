package com.chaos.tablist.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.chaos.tablist.ChaosTablist;
import com.chaos.tablist.config.PngLimits;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/** Iconos PNG que manda el servidor (&lt;mundo&gt;/chaostablist/icons), como texturas para &lt;icon:custom/nombre&gt;. */
public final class CustomIcons {

	public record Icon(Identifier texture, int width, int height) {}

	private static final Map<String, Icon> ICONS = new HashMap<>();

	private CustomIcons() {}

	@Nullable
	public static Icon get(String name) {
		return ICONS.get(name);
	}

	public static Set<String> names() {
		return ICONS.keySet();
	}

	public static void accept(String name, byte[] png) {
		if ("*".equals(name)) {
			clear();
			return;
		}
		try {
			if (!PngLimits.validName(name) || !ICONS.containsKey(name) && ICONS.size() >= PngLimits.MAX_ICONS)
                throw new IllegalArgumentException("Invalid icon name or icon limit reached");
            PngLimits.validate(png);
            NativeImage image = NativeImage.read(png);
			Identifier id = ChaosTablist.id("custom_icon/" + name);
			int width = image.getWidth(), height = image.getHeight();
            DynamicTexture texture;
            try { texture = new DynamicTexture(() -> "ChaosTablist icon", image); }
            catch (RuntimeException e) { image.close(); throw e; }
            try {
                if (ICONS.containsKey(name)) Minecraft.getInstance().getTextureManager().release(id);
                Minecraft.getInstance().getTextureManager().register(id, texture);
                ICONS.put(name, new Icon(id, width, height));
            } catch (RuntimeException e) { texture.close(); ICONS.remove(name); throw e; }
		} catch (Exception e) {
			ChaosTablist.LOGGER.warn("Icono propio inválido: {}", name, e);
		}
	}

	public static void clear() {
		for (Icon icon : ICONS.values()) {
			Minecraft.getInstance().getTextureManager().release(icon.texture());
		}
		ICONS.clear();
	}
}
