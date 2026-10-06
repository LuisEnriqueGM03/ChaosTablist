package com.chaos.tablist.text;

import java.util.List;

import net.minecraft.network.chat.Component;

/**
 * Colores que se pueden elegir en el editor, ordenados como se ven en la rejilla (cada fila es una familia:
 * rojos, naranjas, amarillos, verdes, azules, morados, rosas, marrones, grises y marcas).
 * El nombre se traduce con chaostablist.color.&lt;id&gt;.
 */
public final class Palette {

	public record Color(String id, int rgb) {

		public Component name() {
			return Component.translatable("chaostablist.color." + id);
		}
	}

	public static final List<Color> COLORS = List.of(
			// Rojos y naranjas
			new Color("dark_red", 0xB3131B), new Color("red", 0xFF3B3B), new Color("light_red", 0xFF7B7B),
			new Color("coral", 0xFF6F61), new Color("salmon", 0xFFA07A), new Color("orange", 0xFF8A1F),
			new Color("light_orange", 0xFFB35C), new Color("copper", 0xD9773F),
			// Amarillos y dorados
			new Color("gold", 0xFFC300), new Color("amber", 0xFFAA00), new Color("yellow", 0xFFE14D),
			new Color("light_yellow", 0xFFF59D), new Color("cream", 0xFFF3D6), new Color("mustard", 0xC9A227),
			// Verdes
			new Color("lime", 0x7CE01A), new Color("neon_green", 0x39FF14), new Color("mint", 0x98FFB3),
			new Color("emerald", 0x17C45A), new Color("green", 0x3C8A2E), new Color("dark_green", 0x1E5A1E),
			new Color("olive", 0x7A8A2E), new Color("turquoise", 0x22D3C5),
			// Azules
			new Color("diamond", 0x3FD8EA), new Color("cyan", 0x1599B0), new Color("sky", 0x87CEFA),
			new Color("light_blue", 0x4AA3FF), new Color("blue", 0x2F5BFF), new Color("lapis", 0x2440A8),
			new Color("navy", 0x1A237E), new Color("ice", 0xCDEFFF),
			// Morados y rosas
			new Color("purple", 0x9B36E8), new Color("amethyst", 0xB57CF0), new Color("lavender", 0xD7B8FF),
			new Color("violet", 0x6A1FB0), new Color("magenta", 0xE23BD9), new Color("pink", 0xFF6FB5),
			new Color("light_pink", 0xFFB8DA), new Color("hot_pink", 0xFF1F8E),
			// Marrones y tierra
			new Color("brown", 0x8B5A2F), new Color("dark_brown", 0x5A3A1A), new Color("sand", 0xE0C890),
			new Color("tan", 0xC8A27A), new Color("rust", 0xA0462A), new Color("wine", 0x7A1F3A),
			// Grises
			new Color("white", 0xFFFFFF), new Color("light_gray", 0xC0C0C0), new Color("silver", 0xA8A8A8),
			new Color("gray", 0x7A7A7A), new Color("dark_gray", 0x4A4A4A), new Color("charcoal", 0x2E2E2E),
			new Color("black", 0x1E1E1E), new Color("obsidian", 0x3B2A5C),
			// Marcas
			new Color("youtube", 0xFF0000), new Color("twitch", 0x9146FF), new Color("discord", 0x5865F2),
			new Color("kick", 0x53FC18), new Color("spotify", 0x1DB954), new Color("tiktok", 0x25F4EE),
			new Color("instagram", 0xE1306C), new Color("twitter", 0x1DA1F2));

	private Palette() {}
}
