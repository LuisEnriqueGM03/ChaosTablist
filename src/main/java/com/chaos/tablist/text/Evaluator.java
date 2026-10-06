package com.chaos.tablist.text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Evalúa una plantilla en un instante dado: resuelve placeholders y aplica colores, estilos y efectos.
 * El mismo código corre en el servidor (para los clientes vanilla) y en el cliente con mod (a cada frame),
 * así que las animaciones se ven iguales; el cliente además usa desplazamientos, transparencia y fondos.
 */
public final class Evaluator {

	/** Alineación pedida con &lt;left&gt;, &lt;center&gt; o &lt;right&gt; (UNSET si no se pidió). */
	public record Line(List<Glyph> glyphs, int align) {
		public static final int LEFT = -1;
		public static final int CENTER = 0;
		public static final int RIGHT = 1;
		public static final int UNSET = 2;
	}

	private static final int MAX_DEPTH = 6;
	private static final int MAX_GLYPHS = 2048;
	private static final String SMALL_CAPS_FROM = "abcdefghijklmnopqrstuvwxyz";
	private static final String SMALL_CAPS_TO = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";
	private static final Map<String, Component> JSON_CACHE = new ConcurrentHashMap<>();

	private final TextContext ctx;
	private final double t;
	private int unit;
	private int depth;
	private int count;
	private int align = Line.UNSET;

	private Evaluator(TextContext ctx) {
		this.ctx = ctx;
		this.t = ctx.time();
	}

	public static List<Glyph> eval(String template, TextContext ctx) {
		return line(template, ctx).glyphs();
	}

	public static Line line(String template, TextContext ctx) {
		Evaluator e = new Evaluator(ctx);
		List<Glyph> out = e.nodes(ChaosText.parse(template));
		return new Line(out, e.align);
	}

	// ---------------------------------------------------------------------------------------------
	// Árbol

	private List<Glyph> nodes(List<Node> nodes) {
		List<Glyph> out = new ArrayList<>();
		for (Node n : nodes) {
			if (count > MAX_GLYPHS) {
				break;
			}
			switch (n) {
				case Node.Text text -> text(text.text(), out);
				case Node.Placeholder p -> placeholder(p.key(), out);
				case Node.Tag tag -> out.addAll(tag(tag));
			}
		}
		return out;
	}

	private void text(String s, List<Glyph> out) {
		s.codePoints().forEach(cp -> {
			out.add(new Glyph(new String(Character.toChars(cp)), unit++));
			count++;
		});
	}

	private void placeholder(String key, List<Glyph> out) {
		if (key.startsWith("anim:")) {
			String frame = ctx.animationFrame(key.substring(5), t);
			if (frame != null && depth < MAX_DEPTH) {
				depth++;
				out.addAll(nodes(ChaosText.parse(frame)));
				depth--;
			}
			return;
		}
		String value = ctx.value(key);
		if (value.startsWith(TextContext.JSON_MARK)) {
			Component c = json(value.substring(TextContext.JSON_MARK.length()));
			if (c != null) {
				int u = unit++;
				// Los clientes vanilla no tienen las fuentes de otros mods: ven "[NOMBRE]" (si existe {clave_name}).
				String label = ctx.value(key + "_name");
				if (!label.isEmpty()) {
					Glyph fb = new Glyph("", u);
					fb.fallback = "[" + label + "]";
					out.add(fb);
				}
				c.visit((style, str) -> {
					str.codePoints().forEach(cp -> {
						Glyph g = new Glyph(new String(Character.toChars(cp)), u).style(style);
						if (g.font != null && !label.isEmpty()) {
							g.fallback = "";
						}
						if (fb0(out, u) != null && g.color != Glyph.UNSET) {
							fb0(out, u).color = fb0(out, u).color == Glyph.UNSET ? g.color : fb0(out, u).color;
						}
						out.add(g);
						count++;
					});
					return java.util.Optional.empty();
				}, Style.EMPTY);
			}
			return;
		}
		text(value, out);
	}

	/** El glifo de respaldo de la unidad {@code u}, si es el primero de la lista con esa unidad. */
	@Nullable
	private static Glyph fb0(List<Glyph> out, int u) {
		for (int i = out.size() - 1; i >= 0; i--) {
			Glyph g = out.get(i);
			if (g.unit != u) {
				return null;
			}
			if (g.kind == Glyph.Kind.TEXT && g.text.isEmpty() && g.fallback != null && !g.fallback.isEmpty()) {
				return g;
			}
		}
		return null;
	}

	@Nullable
	private Component json(String json) {
		Component cached = JSON_CACHE.get(json);
		if (cached != null || ctx.registries() == null) {
			return cached;
		}
		try {
			Component c = Component.Serializer.fromJson(json, ctx.registries());
			if (JSON_CACHE.size() > 512) {
				JSON_CACHE.clear();
			}
			if (c != null) {
				JSON_CACHE.put(json, c);
			}
			return c;
		} catch (Exception e) {
			return null;
		}
	}

	/** Sustituye los {placeholders} dentro de un argumento por su texto plano. */
	private String arg(String raw) {
		if (raw.indexOf('{') < 0) {
			return raw;
		}
		StringBuilder sb = new StringBuilder();
		int i = 0;
		while (i < raw.length()) {
			int open = raw.indexOf('{', i);
			int close = open < 0 ? -1 : raw.indexOf('}', open);
			if (open < 0 || close < 0) {
				sb.append(raw, i, raw.length());
				break;
			}
			sb.append(raw, i, open);
			String v = ctx.value(raw.substring(open + 1, close).toLowerCase(Locale.ROOT));
			if (v.startsWith(TextContext.JSON_MARK)) {
				Component c = json(v.substring(TextContext.JSON_MARK.length()));
				v = c == null ? "" : c.getString();
			}
			sb.append(v);
			i = close + 1;
		}
		return sb.toString();
	}

	private String named(Node.Tag tag, String key, int pos, String def) {
		return arg(tag.named(key, pos, def));
	}

	private float num(Node.Tag tag, String key, int pos, float def) {
		try {
			return Float.parseFloat(named(tag, key, pos, String.valueOf(def)).trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static double parseDouble(String s, double def) {
		try {
			return Double.parseDouble(s.trim().replace(',', '.').replaceAll("[^0-9.+\\-eE]", ""));
		} catch (NumberFormatException e) {
			return def;
		}
	}

	/** speed=N o, si no, el último argumento posicional si es un número (&lt;fade:#a:#b:2&gt;). */
	private float speed(Node.Tag tag, float def) {
		String named = tag.named("speed", Integer.MAX_VALUE, null);
		if (named != null) {
			return (float) parseDouble(arg(named), def);
		}
		List<String> pos = tag.positional();
		if (!pos.isEmpty()) {
			String last = arg(pos.get(pos.size() - 1));
			if (ColorUtil.parse(last) == null && last.matches("-?[0-9]+([.,][0-9]+)?")) {
				return (float) parseDouble(last, def);
			}
		}
		return def;
	}

	/** Colores posicionales seguidos (los que no son colores se saltan). */
	private int[] colors(Node.Tag tag, int from) {
		List<Integer> list = new ArrayList<>();
		List<String> pos = tag.positional();
		for (int i = from; i < pos.size(); i++) {
			Integer c = ColorUtil.parse(arg(pos.get(i)));
			if (c != null) {
				list.add(c);
			}
		}
		return list.stream().mapToInt(Integer::intValue).toArray();
	}

	// ---------------------------------------------------------------------------------------------
	// Etiquetas

	private List<Glyph> tag(Node.Tag tag) {
		String name = tag.name();
		// Etiquetas que deciden qué hijos se evalúan.
		switch (name) {
			case "if" -> {
				return ifTag(tag);
			}
			case "cycle" -> {
				return cycle(tag);
			}
			default -> {}
		}
		List<Glyph> self = selfClosing(tag);
		if (self != null) {
			return self;
		}
		List<Glyph> kids = nodes(tag.children());
		if (kids.isEmpty()) {
			return kids;
		}
		boolean client = !ctx.vanilla();
		switch (name) {
			case "color" -> {
				Integer c = ColorUtil.parse(arg(tag.arg(0, "white")));
				if (c != null) {
					for (Glyph g : kids) {
						if (g.color == Glyph.UNSET) {
							g.color = c & 0xFFFFFF;
							g.alpha *= ColorUtil.alpha(c);
						}
					}
				}
			}
			case "b" -> kids.forEach(g -> g.bold = g.bold == null ? Boolean.TRUE : g.bold);
			case "nobold" -> kids.forEach(g -> g.bold = g.bold == null ? Boolean.FALSE : g.bold);
			case "i" -> kids.forEach(g -> g.italic = g.italic == null ? Boolean.TRUE : g.italic);
			case "noitalic" -> kids.forEach(g -> g.italic = g.italic == null ? Boolean.FALSE : g.italic);
			case "u" -> kids.forEach(g -> g.underlined = g.underlined == null ? Boolean.TRUE : g.underlined);
			case "st" -> kids.forEach(g -> g.strikethrough = g.strikethrough == null ? Boolean.TRUE : g.strikethrough);
			case "obf" -> kids.forEach(g -> g.obfuscated = g.obfuscated == null ? Boolean.TRUE : g.obfuscated);
			case "font" -> {
				ResourceLocation font = ResourceLocation.tryParse(arg(String.join(":", tag.args())));
				if (font != null) {
					kids.forEach(g -> g.font = g.font == null ? font : g.font);
				}
			}
			case "gradient" -> gradient(tag, kids);
			case "rainbow" -> rainbow(tag, kids);
			case "fade" -> {
				int[] cs = colors(tag, 0);
				if (cs.length > 0) {
					float speed = speed(tag, 1);
					int c = ColorUtil.stops(cs.length == 1 ? new int[] {cs[0], 0xFFFFFFFF} : cs, ColorUtil.frac(t * speed * 0.5), true);
					setColor(kids, c, false);
				}
			}
			case "pulse" -> {
				int target = ColorUtil.parse(arg(tag.arg(0, "#FFFFFF")), 0xFFFFFFFF);
				float speed = num(tag, "speed", 1, 1);
				float k = (float) (Math.sin(t * speed * Math.PI * 2) + 1) / 2;
				for (Glyph g : kids) {
					int base = g.color == Glyph.UNSET ? 0xFFFFFF : g.color;
					g.color = ColorUtil.lerp(0xFF000000 | base, target, k) & 0xFFFFFF;
				}
			}
			case "sparkle" -> {
				int c = ColorUtil.parse(arg(tag.arg(0, "#FFFFFF")), 0xFFFFFFFF);
				float density = num(tag, "density", 1, 0.15f);
				float speed = num(tag, "speed", 2, 6);
				Map<Integer, Integer> idx = units(kids);
				long frame = (long) Math.floor(t * speed);
				for (Glyph g : kids) {
					long h = mix(idx.get(g.unit) * 31L + frame * 1_000_003L);
					if ((h & 0xFFFF) / 65535f < density) {
						g.color = c & 0xFFFFFF;
					}
				}
			}
			case "pc" -> {
				int ping = (int) parseDouble(arg(tag.arg(0, "{ping}")), -1);
				setColor(kids, 0xFF000000 | ctx.pingColor(ping), true);
			}
			case "scale" -> {
				double v = parseDouble(arg(tag.arg(0, "0")), 0);
				double min = parseDouble(arg(tag.arg(1, "0")), 0);
				double max = parseDouble(arg(tag.arg(2, "100")), 100);
				int[] cs = colors(tag, 3);
				if (cs.length == 0) {
					cs = new int[] {0xFFFF5555, 0xFFFFFF55, 0xFF55FF55};
				}
				float k = max == min ? 0 : (float) ((v - min) / (max - min));
				setColor(kids, ColorUtil.stops(cs, k, false), true);
			}
			case "typewriter" -> {
				return typewriter(tag, kids);
			}
			case "scroll" -> {
				return scroll(tag, kids);
			}
			case "blink" -> {
				float on = num(tag, "on", 0, 0.5f);
				float off = num(tag, "off", 1, 0.5f);
				if (on + off > 0 && t % (on + off) >= on) {
					kids.forEach(g -> g.hidden = true);
				}
			}
			case "upper", "lower", "smallcaps" -> kids.forEach(g -> {
				if (g.kind == Glyph.Kind.TEXT && g.font == null && g.fallback == null) {
					g.text = switch (name) {
						case "upper" -> g.text.toUpperCase(Locale.ROOT);
						case "lower" -> g.text.toLowerCase(Locale.ROOT);
						default -> {
							int i = SMALL_CAPS_FROM.indexOf(g.text.toLowerCase(Locale.ROOT));
							yield i >= 0 && g.text.length() == 1 ? String.valueOf(SMALL_CAPS_TO.charAt(i)) : g.text;
						}
					};
				}
			});
			// ----- Solo con mod en el cliente (en vanilla no hacen nada).
			case "bg" -> {
				if (client) {
					int c = ColorUtil.parse(arg(tag.arg(0, "#80000000")), 0x80000000);
					int pad = (int) num(tag, "pad", 1, 1);
					boolean round = !"false".equalsIgnoreCase(named(tag, "round", 2, "true"));
					int border = ColorUtil.parse(named(tag, "border", 3, ""), 0);
					Glyph.Background bg = new Glyph.Background(c, Mth.clamp(pad, 0, 8), round, border);
					kids.forEach(g -> g.backgrounds.add(bg));
				}
			}
			case "outline" -> {
				int c = ColorUtil.parse(arg(tag.arg(0, "#000000")), 0xFF000000) & 0xFFFFFF;
				kids.forEach(g -> g.outline = g.outline == Glyph.UNSET ? c : g.outline);
			}
			case "glow" -> {
				int c = ColorUtil.parse(arg(tag.arg(0, "#FFFFFF")), 0xFFFFFFFF) & 0xFFFFFF;
				kids.forEach(g -> g.glow = g.glow == Glyph.UNSET ? c : g.glow);
			}
			case "shadow" -> {
				int c = ColorUtil.parse(arg(tag.arg(0, "#000000")), 0xFF000000) & 0xFFFFFF;
				kids.forEach(g -> g.shadow = g.shadow == Glyph.UNSET ? c : g.shadow);
			}
			case "noshadow" -> kids.forEach(g -> g.shadow = g.shadow == Glyph.UNSET ? -2 : g.shadow);
			case "alpha" -> {
				float a = Mth.clamp(num(tag, "value", 0, 1), 0, 1);
				kids.forEach(g -> g.alpha *= a);
			}
			case "fadein" -> {
				float dur = Math.max(0.01f, num(tag, "time", 0, 0.5f));
				float stagger = num(tag, "stagger", 1, 0);
				double since = t - ctx.openedAt();
				Map<Integer, Integer> idx = units(kids);
				for (Glyph g : kids) {
					g.alpha *= Mth.clamp((float) ((since - idx.get(g.unit) * stagger) / dur), 0, 1);
				}
			}
			case "wave" -> {
				float amp = num(tag, "amp", 0, 1.5f);
				float speed = num(tag, "speed", 1, 1);
				float len = num(tag, "len", 2, 0.5f);
				Map<Integer, Integer> idx = units(kids);
				for (Glyph g : kids) {
					g.dy += (float) Math.sin(t * speed * Math.PI * 2 - idx.get(g.unit) * len) * amp;
				}
			}
			case "bounce" -> {
				float amp = num(tag, "amp", 0, 2);
				float speed = num(tag, "speed", 1, 1);
				Map<Integer, Integer> idx = units(kids);
				for (Glyph g : kids) {
					g.dy -= (float) Math.abs(Math.sin(t * speed * Math.PI - idx.get(g.unit) * 0.35)) * amp;
				}
			}
			case "shake" -> {
				float strength = num(tag, "strength", 0, 0.6f);
				long frame = (long) Math.floor(t * 30);
				Map<Integer, Integer> idx = units(kids);
				for (Glyph g : kids) {
					long h = mix(frame * 7919L + idx.get(g.unit) * 104729L);
					g.dx += ((h & 0xFF) / 255f - 0.5f) * 2 * strength;
					g.dy += (((h >> 8) & 0xFF) / 255f - 0.5f) * 2 * strength;
				}
			}
			default -> {}
		}
		return kids;
	}

	/** Etiquetas sin hijos; null si {@code tag} no es una de ellas. */
	@Nullable
	private List<Glyph> selfClosing(Node.Tag tag) {
		List<Glyph> out = new ArrayList<>();
		switch (tag.name()) {
			case "icon" -> {
				String id = arg(tag.arg(0, "star"));
				int color = ColorUtil.parse(arg(tag.arg(1, "")), Glyph.UNSET);
				Icons.Icon icon = Icons.icon(id);
				if (icon != null) {
					out.add(Badges.icon(icon, unit++, ctx.iconFont(), color == Glyph.UNSET ? Glyph.UNSET : color & 0xFFFFFF));
				} else {
					out.add(image(id.startsWith("custom/") ? id.substring(7) : id, 8, 8));
				}
			}
			case "img" -> out.add(image(arg(tag.arg(0, "")), (int) num(tag, "w", 1, 8), (int) num(tag, "h", 2, 8)));
			case "badge" -> {
				String text = named(tag, "text", 0, "");
				int bg = ColorUtil.parse(named(tag, "bg", 1, "#9B36E8"), 0xFF9B36E8) & 0xFFFFFF;
				int fg = ColorUtil.parse(named(tag, "color", 2, "#FFFFFF"), 0xFFFFFFFF) & 0xFFFFFF;
				String icon = named(tag, "icon", 3, "");
				Integer frame = ColorUtil.parse(named(tag, "frame", 4, ""));
				boolean bold = "true".equalsIgnoreCase(named(tag, "bold", 5, "false"));
				int iconColor = ColorUtil.parse(named(tag, "iconcolor", 6, ""), 0xFF000000 | fg) & 0xFFFFFF;
				Badges.build(out, unit++, ctx.iconFont(), text, bg, fg, icon.isEmpty() ? null : icon, iconColor,
						frame == null ? null : frame & 0xFFFFFF, bold);
				count += out.size();
			}
			case "head" -> {
				Glyph g = Glyph.special(Glyph.Kind.HEAD, arg(tag.arg(0, "{player}")), unit++, "");
				g.width = 9;
				g.height = 8;
				out.add(g);
			}
			case "item" -> {
				Glyph g = Glyph.special(Glyph.Kind.ITEM, arg(String.join(":", tag.args())), unit++, "");
				g.width = 9;
				g.height = 8;
				out.add(g);
			}
			case "space" -> {
				int px = Mth.clamp((int) num(tag, "px", 0, 4), -200, 400);
				Glyph g = Glyph.special(Glyph.Kind.SPACE, "", unit++, " ".repeat(Math.max(0, Math.round(px / 4f))));
				g.width = px;
				out.add(g);
			}
			case "bar" -> bar(tag, out);
			case "br" -> out.add(new Glyph("\n", unit++));
			case "center" -> align = Line.CENTER;
			case "left" -> align = Line.LEFT;
			case "right" -> align = Line.RIGHT;
			case "else", "next" -> {}
			default -> {
				return null;
			}
		}
		return out;
	}

	private Glyph image(String name, int w, int h) {
		Glyph g = Glyph.special(Glyph.Kind.IMAGE, name.toLowerCase(Locale.ROOT), unit++, "");
		g.width = Mth.clamp(w, 1, 256);
		g.height = Mth.clamp(h, 1, 256);
		return g;
	}

	private void bar(Node.Tag tag, List<Glyph> out) {
		double value = Mth.clamp(parseDouble(arg(tag.arg(0, "0")), 0), 0, 100);
		int width = Mth.clamp((int) num(tag, "width", 1, 10), 1, 100);
		int full = ColorUtil.parse(arg(tag.arg(2, "#55FF55")), 0xFF55FF55);
		int empty = ColorUtil.parse(arg(tag.arg(3, "#555555")), 0xFF555555);
		String ch = named(tag, "char", 4, "|");
		Integer full2 = ColorUtil.parse(named(tag, "to", 5, ""));
		int filled = (int) Math.round(value / 100 * width);
		for (int i = 0; i < width; i++) {
			Glyph g = new Glyph(ch.isEmpty() ? "|" : ch.substring(0, ch.offsetByCodePoints(0, 1)), unit++);
			int c = i < filled ? full2 == null ? full : ColorUtil.lerp(full, full2, width == 1 ? 0 : i / (float) (width - 1)) : empty;
			g.color = c & 0xFFFFFF;
			out.add(g);
		}
	}

	private List<Glyph> ifTag(Node.Tag tag) {
		List<List<Node>> parts = split(tag.children(), "else");
		String a = arg(tag.arg(0, ""));
		String op = arg(tag.arg(1, "ne")).toLowerCase(Locale.ROOT);
		String b = arg(tag.arg(2, ""));
		boolean result = switch (op) {
			case "eq", "=", "==" -> a.equalsIgnoreCase(b);
			case "ne", "!=" -> !a.equalsIgnoreCase(b);
			case "gt", ">" -> parseDouble(a, 0) > parseDouble(b, 0);
			case "lt", "<" -> parseDouble(a, 0) < parseDouble(b, 0);
			case "ge", ">=" -> parseDouble(a, 0) >= parseDouble(b, 0);
			case "le", "<=" -> parseDouble(a, 0) <= parseDouble(b, 0);
			case "contains" -> a.toLowerCase(Locale.ROOT).contains(b.toLowerCase(Locale.ROOT));
			case "empty" -> a.isEmpty();
			default -> !a.isEmpty();
		};
		if (result) {
			return nodes(parts.get(0));
		}
		return parts.size() > 1 ? nodes(parts.get(1)) : new ArrayList<>();
	}

	private List<Glyph> cycle(Node.Tag tag) {
		List<List<Node>> parts = split(tag.children(), "next");
		float interval = Math.max(0.05f, num(tag, "interval", 0, 2));
		boolean fade = "true".equalsIgnoreCase(named(tag, "fade", 1, "false"));
		long step = (long) Math.floor(t / interval);
		List<Glyph> out = nodes(parts.get((int) (step % parts.size())));
		if (fade && !ctx.vanilla()) {
			double into = t - step * interval;
			float edge = Math.min(0.3f, interval / 4);
			float a = (float) Math.min(1, Math.min(into / edge, (interval - into) / edge));
			out.forEach(g -> g.alpha *= Mth.clamp(a, 0, 1));
		}
		return out;
	}

	private static List<List<Node>> split(List<Node> children, String separator) {
		List<List<Node>> parts = new ArrayList<>();
		List<Node> cur = new ArrayList<>();
		for (Node n : children) {
			if (n instanceof Node.Tag tag && tag.name().equals(separator)) {
				parts.add(cur);
				cur = new ArrayList<>();
			} else {
				cur.add(n);
			}
		}
		parts.add(cur);
		return parts;
	}

	// ---------------------------------------------------------------------------------------------
	// Efectos de color y de texto

	private void gradient(Node.Tag tag, List<Glyph> kids) {
		int[] cs = colors(tag, 0);
		if (cs.length == 0) {
			cs = new int[] {0xFFFFFFFF, 0xFF555555};
		} else if (cs.length == 1) {
			cs = new int[] {cs[0], 0xFFFFFFFF};
		}
		float speed = speed(tag, 0);
		Map<Integer, Integer> idx = units(kids);
		int n = idx.size();
		for (Glyph g : kids) {
			int k = idx.get(g.unit);
			int c;
			if (speed != 0) {
				// Degradado que se desliza: se recorre en bucle (a→b→a) para que no salte.
				int[] loop = new int[cs.length * 2 - 2 > 0 ? cs.length * 2 - 2 : 1];
				for (int i = 0; i < loop.length; i++) {
					loop[i] = i < cs.length ? cs[i] : cs[cs.length * 2 - 2 - i];
				}
				c = ColorUtil.stops(loop, ColorUtil.frac(k / (double) Math.max(1, n) * 0.5 - t * speed * 0.25), true);
			} else {
				c = ColorUtil.stops(cs, n <= 1 ? 0 : k / (float) (n - 1), false);
			}
			if (g.color == Glyph.UNSET) {
				g.color = c & 0xFFFFFF;
				g.alpha *= ColorUtil.alpha(c);
			}
		}
	}

	private void rainbow(Node.Tag tag, List<Glyph> kids) {
		float speed = num(tag, "speed", 0, 0);
		float sat = num(tag, "sat", 1, 0.75f);
		float bright = num(tag, "bright", 2, 1);
		float phase = num(tag, "phase", 3, 0);
		Map<Integer, Integer> idx = units(kids);
		int n = Math.max(1, idx.size());
		for (Glyph g : kids) {
			if (g.color == Glyph.UNSET) {
				float h = ColorUtil.frac(idx.get(g.unit) / (double) n + phase - t * speed * 0.25);
				g.color = ColorUtil.hsv(h, sat, bright) & 0xFFFFFF;
			}
		}
	}

	private List<Glyph> typewriter(Node.Tag tag, List<Glyph> kids) {
		float speed = Math.max(0.1f, num(tag, "speed", 0, 8));
		float pause = num(tag, "pause", 1, 2);
		String cursor = named(tag, "cursor", 2, "");
		Map<Integer, Integer> idx = units(kids);
		int n = idx.size();
		double cycle = n + pause * speed;
		int visible = (int) Math.min(n, Math.floor(t * speed) % Math.max(1, cycle));
		List<Glyph> out = new ArrayList<>();
		Glyph last = null;
		for (Glyph g : kids) {
			if (idx.get(g.unit) < visible) {
				out.add(g);
				last = g;
			}
		}
		if (!cursor.isEmpty() && (visible < n || (t * 2) % 2 < 1)) {
			Glyph c = last != null ? last.copy() : new Glyph("", 0);
			c.kind = Glyph.Kind.TEXT;
			c.text = cursor;
			c.fallback = null;
			c.unit = unit++;
			out.add(c);
		}
		return out;
	}

	private List<Glyph> scroll(Node.Tag tag, List<Glyph> kids) {
		int width = Mth.clamp((int) num(tag, "width", 0, 16), 1, 200);
		float speed = num(tag, "speed", 1, 4);
		int gap = Mth.clamp((int) num(tag, "gap", 2, 4), 0, 50);
		Map<Integer, List<Glyph>> byUnit = new LinkedHashMap<>();
		for (Glyph g : kids) {
			byUnit.computeIfAbsent(g.unit, k -> new ArrayList<>()).add(g);
		}
		List<List<Glyph>> groups = new ArrayList<>(byUnit.values());
		if (groups.size() <= width) {
			return kids;
		}
		Glyph style = kids.get(kids.size() - 1);
		for (int i = 0; i < gap; i++) {
			Glyph sp = style.copy();
			sp.kind = Glyph.Kind.TEXT;
			sp.text = " ";
			sp.fallback = null;
			sp.unit = unit++;
			groups.add(List.of(sp));
		}
		int total = groups.size();
		int offset = (int) (Math.floor(t * speed) % total);
		List<Glyph> out = new ArrayList<>();
		for (int i = 0; i < width; i++) {
			for (Glyph g : groups.get((offset + i) % total)) {
				Glyph c = g.copy();
				c.unit = unit++;
				out.add(c);
			}
		}
		return out;
	}

	private static void setColor(List<Glyph> kids, int argb, boolean onlyUnset) {
		for (Glyph g : kids) {
			if (!onlyUnset || g.color == Glyph.UNSET) {
				g.color = argb & 0xFFFFFF;
			}
		}
	}

	/** Índice (0..n-1) de cada unidad en el orden en que aparece. */
	private static Map<Integer, Integer> units(List<Glyph> glyphs) {
		Map<Integer, Integer> idx = new LinkedHashMap<>();
		for (Glyph g : glyphs) {
			idx.putIfAbsent(g.unit, idx.size());
		}
		return idx;
	}

	private static long mix(long x) {
		x ^= x >>> 33;
		x *= 0xff51afd7ed558ccdL;
		x ^= x >>> 33;
		x *= 0xc4ceb9fe1a85ec53L;
		x ^= x >>> 33;
		return x;
	}

	// ---------------------------------------------------------------------------------------------
	// Salida para clientes vanilla

	/** Component equivalente para clientes sin el mod: iconos y badges pasan a sus símbolos de respaldo. */
	public static MutableComponent toComponent(List<Glyph> glyphs) {
		MutableComponent root = Component.empty();
		StringBuilder run = new StringBuilder();
		Style runStyle = null;
		for (Glyph g : glyphs) {
			if (g.hidden) {
				continue;
			}
			String text = g.fallback != null ? g.fallback : g.kind == Glyph.Kind.TEXT ? g.text : "";
			if (text.isEmpty()) {
				continue;
			}
			Style style = g.mcStyle(true);
			if (g.fallback != null) {
				style = style.withFont(Style.DEFAULT_FONT);
			}
			if (runStyle != null && !style.equals(runStyle)) {
				root.append(Component.literal(run.toString()).setStyle(runStyle));
				run.setLength(0);
			}
			runStyle = style;
			run.append(text);
		}
		if (runStyle != null && !run.isEmpty()) {
			root.append(Component.literal(run.toString()).setStyle(runStyle));
		}
		return root;
	}

	/** Texto plano (para ordenar, buscar o comparar). */
	public static String plain(List<Glyph> glyphs) {
		StringBuilder sb = new StringBuilder();
		for (Glyph g : glyphs) {
			if (g.kind == Glyph.Kind.TEXT && g.font == null) {
				sb.append(g.text);
			}
		}
		return sb.toString();
	}
}
