package com.chaos.tablist.client;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.chaos.tablist.text.Glyph;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Dibuja una lista de glifos: fondos detrás del texto, contorno, brillo, sombra de color, transparencia,
 * desplazamientos por letra, cabezas, ítems e imágenes. Junta los glifos seguidos que se ven igual en un solo
 * trozo de texto para no dibujar letra a letra.
 */
public final class RichText {

	private RichText() {}

	public static float advance(Font font, Glyph g) {
		return switch (g.kind) {
			case TEXT -> g.text.isEmpty() ? 0 : font.width(FormattedText.of(g.text, g.mcStyle(false)));
			case SPACE, HEAD, ITEM -> g.width;
			case IMAGE -> imageWidth(g);
		};
	}

	public static int width(Font font, List<Glyph> glyphs) {
		float w = 0;
		for (Glyph g : glyphs) {
			w += advance(font, g);
		}
		return Mth.ceil(w);
	}

	/** Alto de la línea: 9 px, o más si lleva imágenes altas. */
	public static int height(List<Glyph> glyphs) {
		int h = 9;
		for (Glyph g : glyphs) {
			if (g.kind == Glyph.Kind.IMAGE) {
				h = Math.max(h, imageHeight(g) + 1);
			}
		}
		return h;
	}

	/** Parte en líneas por los saltos (&lt;br&gt;). */
	public static List<List<Glyph>> lines(List<Glyph> glyphs) {
		List<List<Glyph>> out = new ArrayList<>();
		List<Glyph> cur = new ArrayList<>();
		for (Glyph g : glyphs) {
			if (g.kind == Glyph.Kind.TEXT && g.text.equals("\n")) {
				out.add(cur);
				cur = new ArrayList<>();
			} else {
				cur.add(g);
			}
		}
		out.add(cur);
		return out;
	}

	/**
	 * Dibuja los glifos con la parte de arriba del texto en {@code y}. Las imágenes altas crecen hacia arriba
	 * (quedan apoyadas en la línea del texto).
	 */
	public static void draw(GuiGraphics g, Font font, List<Glyph> glyphs, float x, float y, float alphaMul, boolean shadow) {
		if (glyphs.isEmpty() || alphaMul <= 0.01f) {
			return;
		}
		RenderSystem.enableBlend();
		float[] xs = new float[glyphs.size()];
		float cx = x;
		for (int i = 0; i < glyphs.size(); i++) {
			xs[i] = cx;
			cx += advance(font, glyphs.get(i));
		}
		drawBackgrounds(g, font, glyphs, xs, y, alphaMul);

		int i = 0;
		while (i < glyphs.size()) {
			Glyph first = glyphs.get(i);
			if (first.kind != Glyph.Kind.TEXT) {
				drawSpecial(g, first, xs[i], y, alphaMul);
				i++;
				continue;
			}
			int j = i + 1;
			StringBuilder run = new StringBuilder(first.text);
			while (j < glyphs.size() && glyphs.get(j).sameLook(first)) {
				run.append(glyphs.get(j).text);
				j++;
			}
			if (!first.hidden && !run.isEmpty()) {
				drawRun(g, font, first, run.toString(), xs[i], y, alphaMul, shadow);
			}
			i = j;
		}
	}

	private static void drawRun(GuiGraphics g, Font font, Glyph style, String text, float x, float y, float alphaMul,
			boolean shadow) {
		float a = Mth.clamp(style.alpha * alphaMul, 0, 1);
		if (a < 0.02f) {
			return;
		}
		int alpha = Math.max(4, Math.round(a * 255)) << 24;
		int rgb = style.color == Glyph.UNSET ? 0xFFFFFF : style.color;
		FormattedCharSequence seq = FormattedCharSequence.forward(text, style.mcStyle(false));
		float px = x + style.dx;
		float py = y + style.dy;

		if (style.glow != Glyph.UNSET) {
			int glow = Math.max(4, Math.round(a * 90)) << 24 | style.glow;
			for (int[] o : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) {
				text(g, font, seq, px + o[0], py + o[1], glow, false);
			}
		}
		if (style.outline != Glyph.UNSET) {
			int outline = alpha | style.outline;
			for (int[] o : new int[][] {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
				text(g, font, seq, px + o[0], py + o[1], outline, false);
			}
		}
		boolean vanillaShadow = shadow && style.shadow == Glyph.UNSET && style.outline == Glyph.UNSET;
		if (style.shadow >= 0) {
			text(g, font, seq, px + 1, py + 1, alpha | style.shadow, false);
		}
		text(g, font, seq, px, py, alpha | rgb, vanillaShadow);
	}

	private static void text(GuiGraphics g, Font font, FormattedCharSequence seq, float x, float y, int color, boolean shadow) {
		if (x == Math.floor(x) && y == Math.floor(y)) {
			g.drawString(font, seq, (int) x, (int) y, color, shadow);
			return;
		}
		g.pose().pushPose();
		g.pose().translate(x, y, 0);
		g.drawString(font, seq, 0, 0, color, shadow);
		g.pose().popPose();
	}

	private static void drawBackgrounds(GuiGraphics g, Font font, List<Glyph> glyphs, float[] xs, float y, float alphaMul) {
		Map<Glyph.Background, float[]> boxes = new IdentityHashMap<>();
		for (int i = 0; i < glyphs.size(); i++) {
			Glyph gl = glyphs.get(i);
			if (gl.backgrounds.isEmpty()) {
				continue;
			}
			float x0 = xs[i] + gl.dx;
			float x1 = x0 + advance(font, gl);
			for (Glyph.Background bg : gl.backgrounds) {
				float[] box = boxes.computeIfAbsent(bg, k -> new float[] {Float.MAX_VALUE, -Float.MAX_VALUE});
				box[0] = Math.min(box[0], x0);
				box[1] = Math.max(box[1], x1);
			}
		}
		// Los de fuera primero (los que abarcan más), para que los anidados queden encima.
		List<Map.Entry<Glyph.Background, float[]>> list = new ArrayList<>(boxes.entrySet());
		list.sort((a, b) -> Float.compare(b.getValue()[1] - b.getValue()[0], a.getValue()[1] - a.getValue()[0]));
		for (Map.Entry<Glyph.Background, float[]> e : list) {
			Glyph.Background bg = e.getKey();
			int pad = bg.pad();
			int left = Mth.floor(e.getValue()[0]) - pad - 1;
			int right = Mth.ceil(e.getValue()[1]) + pad;
			int top = Mth.floor(y) - pad;
			int bottom = Mth.floor(y) + 8 + pad;
			rect(g, left, top, right, bottom, scaleAlpha(bg.color(), alphaMul), bg.round());
			if (bg.border() != 0) {
				border(g, left, top, right, bottom, scaleAlpha(bg.border(), alphaMul), bg.round());
			}
		}
	}

	/** Rectángulo, con las esquinas recortadas 1 px si {@code round}. */
	public static void rect(GuiGraphics g, int x0, int y0, int x1, int y1, int argb, boolean round) {
		if (!round || x1 - x0 < 3 || y1 - y0 < 3) {
			g.fill(x0, y0, x1, y1, argb);
			return;
		}
		g.fill(x0 + 1, y0, x1 - 1, y0 + 1, argb);
		g.fill(x0, y0 + 1, x1, y1 - 1, argb);
		g.fill(x0 + 1, y1 - 1, x1 - 1, y1, argb);
	}

	public static void border(GuiGraphics g, int x0, int y0, int x1, int y1, int argb, boolean round) {
		int r = round ? 1 : 0;
		g.fill(x0 + r, y0, x1 - r, y0 + 1, argb);
		g.fill(x0 + r, y1 - 1, x1 - r, y1, argb);
		g.fill(x0, y0 + r, x0 + 1, y1 - r, argb);
		g.fill(x1 - 1, y0 + r, x1, y1 - r, argb);
	}

	public static int scaleAlpha(int argb, float mul) {
		int a = Math.round(((argb >>> 24) & 0xFF) * Mth.clamp(mul, 0, 1));
		return a << 24 | (argb & 0xFFFFFF);
	}

	private static void drawSpecial(GuiGraphics g, Glyph gl, float x, float y, float alphaMul) {
		if (gl.hidden || gl.kind == Glyph.Kind.SPACE) {
			return;
		}
		float a = Mth.clamp(gl.alpha * alphaMul, 0, 1);
		if (a < 0.02f) {
			return;
		}
		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(1, 1, 1, a);
		g.pose().pushPose();
		g.pose().translate(x + gl.dx, y + gl.dy, 0);
		switch (gl.kind) {
			case HEAD -> {
				Minecraft mc = Minecraft.getInstance();
				PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(gl.text);
				if (info != null) {
					PlayerFaceRenderer.draw(g, info.getSkin(), 0, 0, 8);
				} else {
					PlayerFaceRenderer.draw(g, net.minecraft.client.resources.DefaultPlayerSkin.get(java.util.UUID.nameUUIDFromBytes(gl.text.getBytes())), 0, 0, 8);
				}
			}
			case ITEM -> {
				ResourceLocation id = ResourceLocation.tryParse(gl.text);
				Item item = id == null ? Items.BARRIER : BuiltInRegistries.ITEM.get(id);
				g.pose().scale(0.5f, 0.5f, 1);
				g.renderItem(new ItemStack(item == Items.AIR ? Items.BARRIER : item), 0, 0);
			}
			case IMAGE -> {
				CustomIcons.Icon icon = CustomIcons.get(gl.text);
				int w = imageWidth(gl);
				int h = imageHeight(gl);
				if (icon != null) {
					g.blit(icon.texture(), 0, 8 - h, w, h, 0, 0, icon.width(), icon.height(), icon.width(), icon.height());
				} else {
					g.fill(0, 8 - h, w, 8, 0x80FF00FF);
				}
			}
			default -> {}
		}
		g.pose().popPose();
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}

	/** Ancho de una imagen: el pedido, o el que corresponde a su alto manteniendo la proporción del PNG. */
	private static int imageWidth(Glyph gl) {
		CustomIcons.Icon icon = CustomIcons.get(gl.text);
		if (icon != null && gl.width == 8 && gl.height != 8 && icon.height() > 0) {
			return Math.max(1, gl.height * icon.width() / icon.height());
		}
		return gl.width;
	}

	private static int imageHeight(Glyph gl) {
		return gl.height;
	}
}
