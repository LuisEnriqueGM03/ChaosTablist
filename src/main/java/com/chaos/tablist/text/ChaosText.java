package com.chaos.tablist.text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parser de plantillas al estilo MiniMessage:
 * <pre>
 *   &lt;#FF5555&gt;rojo&lt;/#FF5555&gt;  &lt;gradient:#f00:#00f:speed=1&gt;texto&lt;/gradient&gt;  &lt;b&gt;negrita&lt;/b&gt;
 *   {placeholder}  {anim:nombre}  &lt;icon:crown:#FFD700&gt;  :heart:  &amp;a legado  &amp;#RRGGBB  \&lt; escape
 * </pre>
 * &lt;/&gt; cierra la última etiqueta abierta. Lo que no es una etiqueta conocida se deja como texto.
 */
public final class ChaosText {

	/** Etiquetas sin hijos. */
	public static final Set<String> SELF_CLOSING = Set.of("icon", "badge", "head", "item", "img", "bar", "space",
			"else", "next", "center", "left", "right", "br");

	/** Etiquetas con hijos (además de los colores). */
	public static final Set<String> CONTAINERS = Set.of("color", "c", "b", "bold", "i", "italic", "u", "underlined",
			"st", "strikethrough", "obf", "obfuscated", "font", "gradient", "rainbow", "fade", "pulse", "typewriter",
			"scroll", "blink", "cycle", "sparkle", "bg", "outline", "shadow", "noshadow", "alpha", "fadein", "wave",
			"shake", "bounce", "glow", "if", "pc", "scale", "upper", "lower", "smallcaps", "noitalic", "nobold");

	private static final String LEGACY = "0123456789abcdefklmnor";
	private static final int CACHE_MAX = 4096;
	private static final Map<String, List<Node>> CACHE = new ConcurrentHashMap<>();

	private ChaosText() {}

	public static List<Node> parse(String template) {
		List<Node> cached = CACHE.get(template);
		if (cached != null) {
			return cached;
		}
		List<Node> nodes = new Parser(template).run();
		if (CACHE.size() > CACHE_MAX) {
			CACHE.clear();
		}
		CACHE.put(template, nodes);
		return nodes;
	}

	/** Texto sin etiquetas (para medir o buscar). */
	public static String strip(String template) {
		StringBuilder sb = new StringBuilder();
		stripInto(parse(template), sb);
		return sb.toString();
	}

	private static void stripInto(List<Node> nodes, StringBuilder sb) {
		for (Node n : nodes) {
			switch (n) {
				case Node.Text t -> sb.append(t.text());
				case Node.Placeholder p -> sb.append('{').append(p.key()).append('}');
				case Node.Tag t -> stripInto(t.children(), sb);
			}
		}
	}

	private static boolean isTagName(String name) {
		return SELF_CLOSING.contains(name) || CONTAINERS.contains(name) || name.startsWith("#")
				|| ColorUtil.isNamed(name);
	}

	private static final class Frame {
		final String name;
		final List<String> args;
		final List<Node> children = new ArrayList<>();
		final boolean legacy;

		Frame(String name, List<String> args, boolean legacy) {
			this.name = name;
			this.args = args;
			this.legacy = legacy;
		}
	}

	private static final class Parser {
		private final String s;
		private final Deque<Frame> stack = new ArrayDeque<>();
		private final List<Node> root = new ArrayList<>();
		private final StringBuilder text = new StringBuilder();

		Parser(String s) {
			this.s = s == null ? "" : s;
		}

		List<Node> current() {
			return stack.isEmpty() ? root : stack.peek().children;
		}

		void flush() {
			if (!text.isEmpty()) {
				current().add(new Node.Text(text.toString()));
				text.setLength(0);
			}
		}

		void push(String name, List<String> args, boolean legacy) {
			flush();
			stack.push(new Frame(name, args, legacy));
		}

		void pop() {
			flush();
			Frame f = stack.pop();
			current().add(new Node.Tag(f.name, List.copyOf(f.args), List.copyOf(f.children)));
		}

		void closeLegacy() {
			while (!stack.isEmpty() && stack.peek().legacy) {
				pop();
			}
		}

		List<Node> run() {
			int n = s.length();
			int i = 0;
			while (i < n) {
				char c = s.charAt(i);
				if (c == '\\' && i + 1 < n) {
					text.append(s.charAt(i + 1));
					i += 2;
					continue;
				}
				if (c == '<') {
					int end = tagEnd(i);
					if (end > 0 && handleTag(s.substring(i + 1, end))) {
						i = end + 1;
						continue;
					}
				}
				if (c == '{') {
					int end = s.indexOf('}', i);
					if (end > i + 1) {
						String key = s.substring(i + 1, end);
						if (key.matches("[A-Za-z0-9_:.\\-]+")) {
							flush();
							current().add(new Node.Placeholder(key.toLowerCase(Locale.ROOT)));
							i = end + 1;
							continue;
						}
					}
				}
				if ((c == '&' || c == '§') && i + 1 < n) {
					int used = legacy(i);
					if (used > 0) {
						i += used;
						continue;
					}
				}
				if (c == ':') {
					int end = s.indexOf(':', i + 1);
					if (end > i + 1 && end - i < 24) {
						String id = s.substring(i + 1, end);
						if (Icons.icon(id) != null) {
							flush();
							current().add(new Node.Tag("icon", List.of(id), List.of()));
							i = end + 1;
							continue;
						}
					}
				}
				text.append(c);
				i++;
			}
			while (!stack.isEmpty()) {
				pop();
			}
			flush();
			return List.copyOf(root);
		}

		/** Posición del '>' que cierra la etiqueta (saltando {placeholders}), o -1. */
		int tagEnd(int start) {
			int depth = 0;
			for (int j = start + 1; j < s.length() && j - start < 256; j++) {
				char c = s.charAt(j);
				if (c == '{') {
					depth++;
				} else if (c == '}') {
					depth = Math.max(0, depth - 1);
				} else if (c == '<' && depth == 0) {
					return -1;
				} else if (c == '>' && depth == 0) {
					return j;
				}
			}
			return -1;
		}

		boolean handleTag(String body) {
			if (body.isEmpty()) {
				return false;
			}
			if (body.charAt(0) == '/') {
				String name = body.substring(1).toLowerCase(Locale.ROOT);
				if (name.isEmpty()) {
					if (!stack.isEmpty()) {
						pop();
					}
					return true;
				}
				name = alias(name.split(":", 2)[0]);
				if (name.startsWith("#") || (ColorUtil.isNamed(name) && !CONTAINERS.contains(name))) {
					name = "color";
				}
				for (Frame f : stack) {
					if (f.name.equals(name)) {
						while (!stack.peek().name.equals(name)) {
							pop();
						}
						pop();
						return true;
					}
				}
				return isTagName(name);
			}
			List<String> parts = splitArgs(body);
			String name = alias(parts.get(0).toLowerCase(Locale.ROOT));
			List<String> args = new ArrayList<>(parts.subList(1, parts.size()));
			if (name.equals("reset") || name.equals("r")) {
				while (!stack.isEmpty()) {
					pop();
				}
				return true;
			}
			if (name.startsWith("#") || (ColorUtil.isNamed(name) && !CONTAINERS.contains(name))) {
				if (ColorUtil.parse(name) == null) {
					return false;
				}
				args.add(0, name);
				push("color", args, false);
				return true;
			}
			if (SELF_CLOSING.contains(name)) {
				flush();
				current().add(new Node.Tag(name, List.copyOf(args), List.of()));
				return true;
			}
			if (CONTAINERS.contains(name)) {
				push(name, args, false);
				return true;
			}
			return false;
		}

		int legacy(int i) {
			char code = Character.toLowerCase(s.charAt(i + 1));
			if (code == '#' && i + 8 <= s.length()) {
				String hex = s.substring(i + 2, i + 8);
				if (hex.matches("[0-9a-fA-F]{6}")) {
					closeLegacy();
					push("color", List.of("#" + hex), true);
					return 8;
				}
				return 0;
			}
			if (LEGACY.indexOf(code) < 0) {
				return 0;
			}
			switch (code) {
				case 'r' -> closeLegacy();
				case 'k' -> push("obf", List.of(), true);
				case 'l' -> push("b", List.of(), true);
				case 'm' -> push("st", List.of(), true);
				case 'n' -> push("u", List.of(), true);
				case 'o' -> push("i", List.of(), true);
				default -> {
					closeLegacy();
					net.minecraft.ChatFormatting f = net.minecraft.ChatFormatting.getByCode(code);
					push("color", List.of(f != null ? f.getName() : "white"), true);
				}
			}
			return 2;
		}
	}

	private static String alias(String name) {
		return switch (name) {
			case "bold" -> "b";
			case "italic", "em" -> "i";
			case "underlined" -> "u";
			case "strikethrough" -> "st";
			case "obfuscated" -> "obf";
			case "c", "colour" -> "color";
			case "rainbow" -> "rainbow";
			default -> name;
		};
	}

	/** Separa por ':' sin cortar dentro de {placeholders}. */
	static List<String> splitArgs(String body) {
		List<String> out = new ArrayList<>();
		StringBuilder cur = new StringBuilder();
		int depth = 0;
		for (int i = 0; i < body.length(); i++) {
			char c = body.charAt(i);
			if (c == '{') {
				depth++;
			} else if (c == '}') {
				depth = Math.max(0, depth - 1);
			}
			if (c == ':' && depth == 0) {
				out.add(cur.toString());
				cur.setLength(0);
			} else {
				cur.append(c);
			}
		}
		out.add(cur.toString());
		return out;
	}
}
