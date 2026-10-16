package com.chaos.tablist.text;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/**
 * Un carácter (o elemento especial: cabeza, ítem, imagen, espacio) ya evaluado, con todo su estilo.
 * El servidor convierte la lista a un Component para los clientes vanilla; el cliente con mod la dibuja
 * glifo a glifo para poder hacer fondos, transparencia y desplazamientos (ola, temblor...).
 */
public final class Glyph {

	public enum Kind { TEXT, SPACE, HEAD, ITEM, IMAGE }

	public static final int UNSET = -1;

	public Kind kind = Kind.TEXT;
	/** Texto del glifo (normalmente un codepoint) o el argumento del especial (nombre, id del ítem, imagen). */
	public String text;
	/** Lo que ven los clientes vanilla en lugar de este glifo (iconos, cabezas...). null = el propio texto. */
	@Nullable public String fallback;
	/** Glifos con la misma unidad se animan juntos (las capas de un badge, por ejemplo). */
	public int unit;

	public int color = UNSET;
	public float alpha = 1;
	@Nullable public Boolean bold;
	@Nullable public Boolean italic;
	@Nullable public Boolean underlined;
	@Nullable public Boolean strikethrough;
	@Nullable public Boolean obfuscated;
	@Nullable public ResourceLocation font;

	// Solo cliente con mod.
	public float dx;
	public float dy;
	/** UNSET = sombra normal, -2 = sin sombra, otro = color de la sombra. */
	public int shadow = UNSET;
	public int outline = UNSET;
	public int glow = UNSET;
	public boolean hidden;
	/** Ancho en píxeles de SPACE, o ancho/alto de IMAGE. */
	public int width;
	public int height;
	public final List<Background> backgrounds = new ArrayList<>(0);

	/** Fondo detrás de un tramo de texto; los glifos que lo comparten definen su caja. */
	public record Background(int color, int pad, boolean round, int border) {}

	public Glyph(String text, int unit) {
		this.text = text;
		this.unit = unit;
	}

	public static Glyph special(Kind kind, String arg, int unit, String fallback) {
		Glyph g = new Glyph(arg, unit);
		g.kind = kind;
		g.fallback = fallback;
		return g;
	}

	/** Copia el estilo de un Component (rangos de Chaos Ranks, valores JSON). */
	public Glyph style(Style s) {
		if (s.getColor() != null) {
			color = s.getColor().getValue();
		}
		if (s.isBold()) {
			bold = true;
		}
		if (s.isItalic()) {
			italic = true;
		}
		if (s.isUnderlined()) {
			underlined = true;
		}
		if (s.isStrikethrough()) {
			strikethrough = true;
		}
		if (s.isObfuscated()) {
			obfuscated = true;
		}
		if (!Style.DEFAULT_FONT.equals(s.getFont())) {
			font = s.getFont();
		}
		return this;
	}

	/** Estilo de Minecraft (sin color: el color lo pone quien dibuja, con su transparencia). */
	public Style mcStyle(boolean withColor) {
		Style s = Style.EMPTY;
		if (withColor && color != UNSET) {
			s = s.withColor(color);
		}
		if (bold != null) {
			s = s.withBold(bold);
		}
		if (italic != null) {
			s = s.withItalic(italic);
		}
		if (underlined != null) {
			s = s.withUnderlined(underlined);
		}
		if (strikethrough != null) {
			s = s.withStrikethrough(strikethrough);
		}
		if (obfuscated != null) {
			s = s.withObfuscated(obfuscated);
		}
		if (font != null) {
			s = s.withFont(font);
		}
		return s;
	}

	public Glyph copy() {
		Glyph g = new Glyph(text, unit);
		g.kind = kind;
		g.fallback = fallback;
		g.color = color;
		g.alpha = alpha;
		g.bold = bold;
		g.italic = italic;
		g.underlined = underlined;
		g.strikethrough = strikethrough;
		g.obfuscated = obfuscated;
		g.font = font;
		g.dx = dx;
		g.dy = dy;
		g.shadow = shadow;
		g.outline = outline;
		g.glow = glow;
		g.hidden = hidden;
		g.width = width;
		g.height = height;
		g.backgrounds.addAll(backgrounds);
		return g;
	}

	/** Mismo aspecto (para juntar glifos seguidos en un solo trozo de texto al dibujar). */
	public boolean sameLook(Glyph o) {
		return kind == Kind.TEXT && o.kind == Kind.TEXT && color == o.color && alpha == o.alpha && bold == o.bold
				&& italic == o.italic && underlined == o.underlined && strikethrough == o.strikethrough
				&& obfuscated == o.obfuscated && java.util.Objects.equals(font, o.font) && dx == o.dx && dy == o.dy
				&& shadow == o.shadow && outline == o.outline && glow == o.glow && hidden == o.hidden;
	}
}
