package com.chaos.tablist.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.chaos.tablist.ChaosTablist;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** Iconos PNG que manda el servidor (&lt;mundo&gt;/chaostablist/icons), como texturas para &lt;icon:custom/nombre&gt;. */
public final class CustomIcons {

	public record Icon(ResourceLocation texture, int width, int height) {}

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
		if (name.equals("*")) {
			clear();
			return;
		}
		try {
			NativeImage image = NativeImage.read(png);
			ResourceLocation id = ChaosTablist.id("custom_icon/" + name);
			Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
			ICONS.put(name, new Icon(id, image.getWidth(), image.getHeight()));
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
