package com.chaos.tablist.text;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

/**
 * Badge en pixel art (igual que las etiquetas de Chaos Ranks), en tres capas que se dibujan una encima de otra
 * retrocediendo con espacios negativos: borde → fondo interior con el hueco de cada letra → icono y letras.
 * Todo son glifos de la misma unidad, así que los efectos (ola, temblor...) mueven el badge entero.
 */
public final class Badges {

	// Codepoints de assets/chaostablist/font/badge.json (ver tools/gen_font.py).
	private static final int RING_FILL = 0xE000;
	private static final char RING_CAP = '';
	private static final int INNER_FILL = 0xE010;
	private static final int NEG_SPACE = 0xE020;
	private static final int POS_SPACE = 0xE030;
	private static final char NEG_1 = (char) NEG_SPACE;
	private static final int MAX_FILL_BIT = 5;
	private static final int MAX_SPACE_BIT = 7;

	private static final int PAD = 2;
	private static final int ICON_GAP = 2;

	private Badges() {}

	/**
	 * Añade a {@code out} los glifos del badge. Los clientes vanilla ven "[ICONO TEXTO]" en el color del fondo.
	 */
	public static void build(List<Glyph> out, int unit, ResourceLocation font, String rawLabel, int bgColor,
			int textColor, @Nullable String iconId, int iconColor, @Nullable Integer frameColor, boolean bold) {
		String label = Icons.sanitize(rawLabel);
		Icons.Icon icon = Icons.icon(iconId);

		// Lo que ven los clientes sin el mod.
		Glyph vanilla = new Glyph("", unit);
		vanilla.fallback = "[" + (icon != null ? icon.fallback() + (label.isEmpty() ? "" : " ") : "") + label + "]";
		vanilla.color = bgColor;
		vanilla.bold = true;
		out.add(vanilla);

		int labelPx = 0;
		for (char c : label.toCharArray()) {
			labelPx += advance(c, bold);
		}
		if (!label.isEmpty()) {
			labelPx--;
		}
		int iconPx = icon == null ? 0 : icon.width() + (label.isEmpty() ? 0 : ICON_GAP);
		int total = 1 + PAD + iconPx + labelPx + PAD + 1;

		StringBuilder ring = new StringBuilder();
		ring.append(RING_CAP).append(NEG_1);
		fill(ring, RING_FILL, total - 2);
		ring.append(RING_CAP).append(NEG_1);
		ring.append(space(-total));
		seg(out, unit, font, ring, frameColor != null ? frameColor : bgColor);

		StringBuilder bg = new StringBuilder(space(1));
		int x = 1;
		fill(bg, INNER_FILL, PAD);
		x += PAD;
		if (icon != null) {
			bg.append(icon.knockout()).append(NEG_1);
			x += icon.width() + 1;
			if (!label.isEmpty()) {
				fill(bg, INNER_FILL, ICON_GAP - 1);
				x += ICON_GAP - 1;
			}
		}
		for (char c : label.toCharArray()) {
			Icons.Letter l = Icons.letter(c);
			if (l == null) {
				fill(bg, INNER_FILL, Icons.labelSpace());
			} else {
				bg.append(bold ? l.boldKnockout() : l.knockout()).append(NEG_1);
			}
			x += advance(c, bold);
		}
		fill(bg, INNER_FILL, total - 1 - x);
		bg.append(space(-(total - 1)));
		seg(out, unit, font, bg, bgColor);

		x = 1 + PAD;
		seg(out, unit, font, new StringBuilder(space(x)), 0xFFFFFF);
		if (icon != null) {
			StringBuilder s = new StringBuilder().append(icon.glyph());
			x += icon.width() + 1;
			if (!label.isEmpty()) {
				s.append(space(ICON_GAP - 1));
				x += ICON_GAP - 1;
			}
			seg(out, unit, font, s, iconColor);
		}
		if (!label.isEmpty()) {
			StringBuilder s = new StringBuilder();
			for (char c : label.toCharArray()) {
				Icons.Letter l = Icons.letter(c);
				s.append(l == null ? ' ' : bold ? l.boldGlyph() : l.glyph());
				x += advance(c, bold);
			}
			seg(out, unit, font, s, textColor);
		}
		seg(out, unit, font, new StringBuilder(space(total - x)), 0xFFFFFF);
	}

	/** Un icono suelto, del color indicado. */
	public static Glyph icon(Icons.Icon icon, int unit, ResourceLocation font, int color) {
		Glyph g = new Glyph(String.valueOf(icon.glyph()), unit);
		g.font = font;
		g.fallback = icon.fallback();
		if (color != Glyph.UNSET) {
			g.color = color;
		}
		return g;
	}

	private static int advance(char c, boolean bold) {
		Icons.Letter l = Icons.letter(c);
		if (l == null) {
			return Icons.labelSpace();
		}
		return (bold ? l.boldWidth() : l.width()) + 1;
	}

	private static void seg(List<Glyph> out, int unit, ResourceLocation font, CharSequence text, int color) {
		for (int i = 0; i < text.length(); i++) {
			Glyph g = new Glyph(String.valueOf(text.charAt(i)), unit);
			g.font = font;
			g.color = color & 0xFFFFFF;
			g.fallback = "";
			g.italic = false;
			g.bold = false;
			g.shadow = -2;
			out.add(g);
		}
	}

	/** Relleno de {@code width} píxeles con piezas de 1..32; cada pieza avanza un píxel de más y se compensa. */
	private static void fill(StringBuilder sb, int base, int width) {
		for (int bit = MAX_FILL_BIT; bit >= 0; bit--) {
			int w = 1 << bit;
			while (width >= w) {
				sb.append((char) (base + bit)).append(NEG_1);
				width -= w;
			}
		}
	}

	private static String space(int delta) {
		StringBuilder sb = new StringBuilder();
		int base = delta < 0 ? NEG_SPACE : POS_SPACE;
		int left = Math.abs(delta);
		for (int bit = MAX_SPACE_BIT; bit >= 0; bit--) {
			int w = 1 << bit;
			while (left >= w) {
				sb.append((char) (base + bit));
				left -= w;
			}
		}
		return sb.toString();
	}
}
