package com.chaos.tablist.text;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;

/** Colores: #RGB, #RRGGBB, #AARRGGBB, nombres de Minecraft y de la paleta; mezclas e interpolación. */
public final class ColorUtil {

	private static final Map<String, Integer> NAMES = new HashMap<>();

	static {
		for (ChatFormatting f : ChatFormatting.values()) {
			if (f.isColor() && f.getColor() != null) {
				NAMES.put(f.getName(), 0xFF000000 | f.getColor());
			}
		}
		NAMES.put("grey", NAMES.get("gray"));
		NAMES.put("dark_grey", NAMES.get("dark_gray"));
		for (Palette.Color c : Palette.COLORS) {
			NAMES.putIfAbsent(c.id(), 0xFF000000 | c.rgb());
		}
	}

	private ColorUtil() {}

	public static boolean isNamed(String name) {
		return NAMES.containsKey(name.toLowerCase(Locale.ROOT));
	}

	/** Color ARGB (alfa FF si no se indica) o null si no es un color. */
	@Nullable
	public static Integer parse(@Nullable String s) {
		if (s == null) {
			return null;
		}
		s = s.trim();
		if (s.isEmpty()) {
			return null;
		}
		Integer named = NAMES.get(s.toLowerCase(Locale.ROOT));
		if (named != null) {
			return named;
		}
		String hex = s.startsWith("#") ? s.substring(1) : s.startsWith("0x") ? s.substring(2) : null;
		if (hex == null || !hex.matches("[0-9a-fA-F]+")) {
			return null;
		}
		try {
			return switch (hex.length()) {
				case 3 -> {
					int r = Integer.parseInt(hex.substring(0, 1), 16);
					int g = Integer.parseInt(hex.substring(1, 2), 16);
					int b = Integer.parseInt(hex.substring(2, 3), 16);
					yield 0xFF000000 | (r * 17) << 16 | (g * 17) << 8 | b * 17;
				}
				case 6 -> 0xFF000000 | Integer.parseInt(hex, 16);
				case 8 -> (int) Long.parseLong(hex, 16);
				default -> null;
			};
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static int parse(@Nullable String s, int def) {
		Integer c = parse(s);
		return c == null ? def : c;
	}

	public static int rgb(int argb) {
		return argb & 0xFFFFFF;
	}

	public static float alpha(int argb) {
		return ((argb >>> 24) & 0xFF) / 255f;
	}

	public static int lerp(int a, int b, float t) {
		t = Mth.clamp(t, 0, 1);
		int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
		int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
		return Math.round(aa + (ba - aa) * t) << 24 | Math.round(ar + (br - ar) * t) << 16
				| Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
	}

	/** Recorre una lista de colores: pos 0..1 (cíclico si {@code loop}). */
	public static int stops(int[] colors, float pos, boolean loop) {
		if (colors.length == 1) {
			return colors[0];
		}
		if (loop) {
			pos = pos - (float) Math.floor(pos);
			float scaled = pos * colors.length;
			int i = (int) scaled % colors.length;
			return lerp(colors[i], colors[(i + 1) % colors.length], scaled - (int) scaled);
		}
		pos = Mth.clamp(pos, 0, 1);
		float scaled = pos * (colors.length - 1);
		int i = Math.min((int) scaled, colors.length - 2);
		return lerp(colors[i], colors[i + 1], scaled - i);
	}

	/**
	 * Parte decimal calculada en double. El reloj va en segundos desde 1970 (~1.7e9): pasarlo a float antes
	 * de quitarle la parte entera pierde todos los decimales y las animaciones se quedan quietas.
	 */
	public static float frac(double v) {
		return (float) (v - Math.floor(v));
	}

	public static int hsv(float h, float s, float v) {
		return 0xFF000000 | Mth.hsvToRgb(h - (float) Math.floor(h), Mth.clamp(s, 0, 1), Mth.clamp(v, 0, 1));
	}

	public static String hex(int rgb) {
		return String.format("#%06X", rgb & 0xFFFFFF);
	}
}
