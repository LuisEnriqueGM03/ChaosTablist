package com.chaos.tablist.text;

import java.util.List;

/** Árbol de una plantilla ya parseada (ver {@link ChaosText}). */
public sealed interface Node permits Node.Text, Node.Placeholder, Node.Tag {

	record Text(String text) implements Node {}

	/** {clave} o {clave:argumento}, por ejemplo {ping} o {anim:footer}. */
	record Placeholder(String key) implements Node {}

	/** &lt;nombre:arg1:arg2&gt;hijos&lt;/nombre&gt;. Las etiquetas sueltas (icon, badge, else...) no tienen hijos. */
	record Tag(String name, List<String> args, List<Node> children) implements Node {

		public String arg(int i, String def) {
			return i < args.size() && !args.get(i).isEmpty() ? args.get(i) : def;
		}

		/** Argumento con nombre (speed=2) o, si no lo hay, el posicional {@code i}. */
		public String named(String key, int i, String def) {
			for (String a : args) {
				if (a.startsWith(key + "=")) {
					return a.substring(key.length() + 1);
				}
			}
			int pos = 0;
			for (String a : args) {
				if (a.contains("=") && !a.startsWith("#")) {
					continue;
				}
				if (pos++ == i) {
					return a.isEmpty() ? def : a;
				}
			}
			return def;
		}

		/** Argumentos posicionales (sin los que son clave=valor). */
		public List<String> positional() {
			return args.stream().filter(a -> !a.contains("=") || a.startsWith("#")).toList();
		}
	}
}
