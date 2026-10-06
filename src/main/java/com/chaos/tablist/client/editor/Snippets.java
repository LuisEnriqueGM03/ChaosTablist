package com.chaos.tablist.client.editor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * Lo que se puede insertar desde el editor: efectos y placeholders, agrupados por categoría (con su clave de
 * traducción). Cada efecto es una plantilla con marcas {@code $clave$} para sus parámetros y {@code $body$} para
 * el contenido (texto o icono); con los valores por defecto sale el efecto de siempre. En el contenido por
 * defecto, "texto" es lo que se sustituye por la selección al insertar.
 */
public final class Snippets {

	public enum Kind { COLOR, NUMBER, TEXT, CHOICE, BOOL }

	/** Parámetro configurable de un efecto (traducción: chaostablist.param.&lt;key&gt;). */
	public record Param(String key, Kind kind, String def, double min, double max, double step, List<String> options) {}

	/**
	 * @param body contenido por defecto, o null si el efecto no lleva contenido (barra, cabeza, espacio...)
	 */
	public record Snippet(String id, String category, String template, @Nullable String body, List<Param> params) {

		/** Con los valores por defecto (lo que se inserta con un clic). */
		public String text() {
			return build(Map.of(), body);
		}

		public boolean editable() {
			return body != null || !params.isEmpty();
		}

		public Map<String, String> defaults() {
			Map<String, String> out = new LinkedHashMap<>();
			params.forEach(p -> out.put(p.key(), p.def()));
			return out;
		}

		public String build(Map<String, String> values, @Nullable String content) {
			String out = template;
			for (Param p : params) {
				out = out.replace("$" + p.key() + "$", values.getOrDefault(p.key(), p.def()));
			}
			return out.replace("$body$", content == null ? "" : content);
		}
	}

	public record Placeholder(String key, String category) {}

	private static final String T = "texto";

	public static final List<Snippet> EFFECTS = List.of(
			fx("gradient", "color", "<gradient:$c1$:$c2$>$body$</gradient>", T,
					color("c1", "#FF5555"), color("c2", "#5555FF")),
			fx("gradient_anim", "color", "<gradient:$c1$:$c2$:speed=$speed$>$body$</gradient>", T,
					color("c1", "#B57CF0"), color("c2", "#3FD8EA"), num("speed", "1", 0.1, 5, 0.1)),
			fx("rainbow", "color", "<rainbow:$speed$:$sat$>$body$</rainbow>", T,
					num("speed", "1", 0, 5, 0.1), num("sat", "0.75", 0, 1, 0.05)),
			fx("fade", "color", "<fade:$c1$:$c2$:$speed$>$body$</fade>", T,
					color("c1", "#FF5555"), color("c2", "#FFFF55"), num("speed", "1", 0.1, 5, 0.1)),
			fx("pulse", "color", "<pulse:$c1$:$speed$>$body$</pulse>", T,
					color("c1", "#FF3B3B"), num("speed", "1", 0.1, 5, 0.1)),
			fx("sparkle", "color", "<sparkle:$c1$:$density$:$speed$>$body$</sparkle>", T,
					color("c1", "#FFFFFF"), num("density", "0.15", 0.05, 1, 0.05), num("speed", "6", 1, 20, 1)),
			fx("wave", "motion", "<wave:$amp$:$speed$>$body$</wave>", T,
					num("amp", "1.5", 0.5, 5, 0.5), num("speed", "1", 0.1, 5, 0.1)),
			fx("bounce", "motion", "<bounce:$amp$:$speed$>$body$</bounce>", T,
					num("amp", "2", 0.5, 6, 0.5), num("speed", "1", 0.1, 5, 0.1)),
			fx("shake", "motion", "<shake:$strength$>$body$</shake>", T, num("strength", "0.6", 0.1, 3, 0.1)),
			fx("fadein", "motion", "<fadein:$time$:$stagger$>$body$</fadein>", T,
					num("time", "0.4", 0.05, 3, 0.05), num("stagger", "0.05", 0, 0.5, 0.01)),
			fx("typewriter", "animated", "<typewriter:$speed$:$pause$:cursor=$cursor$>$body$</typewriter>", T,
					num("speed", "8", 1, 40, 1), num("pause", "2", 0, 10, 0.5), text("cursor", "_")),
			fx("scroll", "animated", "<scroll:$width$:$speed$>$body$</scroll>", "texto que se desplaza",
					num("width", "10", 1, 60, 1), num("speed", "4", 0.5, 20, 0.5)),
			fx("blink", "animated", "<blink:$on$:$off$>$body$</blink>", T,
					num("on", "0.5", 0.1, 5, 0.1), num("off", "0.5", 0.1, 5, 0.1)),
			fx("cycle", "animated", "<cycle:$interval$:fade=$fade$>$t1$<next>$t2$<next>$t3$</cycle>", null,
					num("interval", "1.5", 0.1, 30, 0.1), bool("fade", "true"), text("t1", "uno"), text("t2", "dos"),
					text("t3", "tres")),
			fx("bold", "style", "<b>$body$</b>", T),
			fx("italic", "style", "<i>$body$</i>", T),
			fx("underlined", "style", "<u>$body$</u>", T),
			fx("strikethrough", "style", "<st>$body$</st>", T),
			fx("obfuscated", "style", "<obf>$body$</obf>", T),
			fx("smallcaps", "style", "<smallcaps>$body$</smallcaps>", T),
			fx("upper", "style", "<upper>$body$</upper>", T),
			fx("bg", "style", "<bg:$c1$:$pad$:$round$>$body$</bg>", T,
					color("c1", "#C0000000"), num("pad", "2", 0, 8, 1), bool("round", "true")),
			fx("bg_border", "style", "<bg:$c1$:$pad$:$round$:$border$>$body$</bg>", T,
					color("c1", "#C02A1606"), num("pad", "2", 0, 8, 1), bool("round", "true"), color("border", "#FFE8B23A")),
			fx("outline", "style", "<outline:$c1$>$body$</outline>", T, color("c1", "#000000")),
			fx("glow", "style", "<glow:$c1$>$body$</glow>", T, color("c1", "#FFD700")),
			fx("shadow", "style", "<shadow:$c1$>$body$</shadow>", T, color("c1", "#5A0F8C")),
			fx("noshadow", "style", "<noshadow>$body$</noshadow>", T),
			fx("alpha", "style", "<alpha:$value$>$body$</alpha>", T, num("value", "0.5", 0.05, 1, 0.05)),
			fx("font", "style", "<font:$font$>$body$</font>", T,
					choice("font", "minecraft:uniform", "minecraft:uniform", "minecraft:alt", "minecraft:illageralt",
							"minecraft:default")),
			fx("bar", "elements", "<bar:$value$:$width$:$c1$:$c2$:char=$char$:to=$to$>", null,
					text("value", "{ram_pct}"), num("width", "10", 1, 40, 1), color("c1", "#55FF55"),
					color("c2", "#3A3A3A"), text("char", "▌"), color("to", "#FF5555")),
			fx("badge", "elements", "<badge:$text$:$bg$:$fg$:$icon$:$frame$:$bold$>", null,
					text("text", "EVENTO"), color("bg", "#7B2CBF"), color("fg", "#FFFFFF"), text("icon", "star"),
					color("frame", "#3B1A5C"), bool("bold", "false")),
			fx("head", "elements", "<head:$player$>", null, text("player", "{player}")),
			fx("item", "elements", "<item:$item$>", null, text("item", "minecraft:diamond_sword")),
			fx("image", "elements", "<icon:custom/$name$>", null, text("name", "logo")),
			fx("image_big", "elements", "<img:$name$:$w$:$h$>", null,
					text("name", "logo"), num("w", "64", 1, 256, 1), num("h", "24", 1, 256, 1)),
			fx("space", "elements", "<space:$px$>", null, num("px", "6", -50, 200, 1)),
			fx("br", "elements", "<br>", null),
			fx("left", "elements", "<left>", null),
			fx("center", "elements", "<center>", null),
			fx("right", "elements", "<right>", null),
			fx("pc", "logic", "<pc:{ping}>$body$</pc>", "{ping}ms"),
			fx("scale", "logic", "<scale:$value$:$min$:$max$:$c1$:$c2$:$c3$>$body$</scale>", "{tps}",
					text("value", "{tps}"), num("min", "15", -1000, 1000, 1), num("max", "20", -1000, 1000, 1),
					color("c1", "#FF5555"), color("c2", "#FFFF55"), color("c3", "#55FF55")),
			fx("if", "logic", "<if:$a$:$op$:$b$>$yes$<else>$no$</if>", null,
					text("a", "{op}"), choice("op", "eq", "eq", "ne", "gt", "lt", "ge", "le", "contains", "empty"),
					text("b", "true"), text("yes", "admin"), text("no", "jugador")),
			fx("legacy", "logic", "&a&lverde &r&#FF55FFrosa", null));

	public static final List<Placeholder> PLACEHOLDERS = List.of(
			new Placeholder("server_name", "server"), new Placeholder("online", "server"),
			new Placeholder("max_players", "server"), new Placeholder("tps", "server"), new Placeholder("mspt", "server"),
			new Placeholder("ram", "server"), new Placeholder("ram_pct", "server"), new Placeholder("ram_used", "server"),
			new Placeholder("ram_max", "server"), new Placeholder("cpu", "server"), new Placeholder("uptime", "server"),
			new Placeholder("date", "server"), new Placeholder("time", "server"), new Placeholder("day", "server"),
			new Placeholder("world_time", "server"), new Placeholder("weather", "server"),
			new Placeholder("weather_icon", "server"), new Placeholder("is_day", "server"),
			new Placeholder("moon_phase", "server"), new Placeholder("motd", "server"), new Placeholder("version", "server"),
			new Placeholder("staff_online", "server"), new Placeholder("afk_online", "server"),
			new Placeholder("difficulty", "server"), new Placeholder("view_distance", "server"),
			new Placeholder("sim_distance", "server"), new Placeholder("entities", "server"), new Placeholder("chunks", "server"),
			new Placeholder("worlds", "server"), new Placeholder("threads", "server"), new Placeholder("java_version", "server"),
			new Placeholder("server_mods", "server"), new Placeholder("port", "server"),
			new Placeholder("player", "player"), new Placeholder("ping", "player"),
			new Placeholder("world", "player"), new Placeholder("dimension", "player"), new Placeholder("biome", "player"),
			new Placeholder("x", "player"), new Placeholder("y", "player"), new Placeholder("z", "player"),
			new Placeholder("health", "player"), new Placeholder("max_health", "player"), new Placeholder("food", "player"),
			new Placeholder("level", "player"), new Placeholder("gamemode", "player"), new Placeholder("deaths", "player"),
			new Placeholder("kills", "player"), new Placeholder("mob_kills", "player"), new Placeholder("playtime", "player"),
			new Placeholder("afk", "player"), new Placeholder("op", "player"), new Placeholder("team", "player"),
			new Placeholder("display_name", "player"), new Placeholder("xp", "player"), new Placeholder("xp_pct", "player"),
			new Placeholder("armor", "player"), new Placeholder("saturation", "player"), new Placeholder("air", "player"),
			new Placeholder("light", "player"), new Placeholder("facing", "player"), new Placeholder("chunk_x", "player"),
			new Placeholder("chunk_z", "player"), new Placeholder("held_item", "player"), new Placeholder("language", "player"),
			new Placeholder("jumps", "player"), new Placeholder("fish", "player"), new Placeholder("walked", "player"),
			new Placeholder("since_death", "player"),
			new Placeholder("fps", "client"), new Placeholder("client_ram", "client"),
			new Placeholder("client_ram_pct", "client"), new Placeholder("my_ping", "client"),
			new Placeholder("local_time", "client"), new Placeholder("local_time_s", "client"),
			new Placeholder("local_date", "client"), new Placeholder("local_weekday", "client"),
			new Placeholder("render_distance", "client"), new Placeholder("resolution", "client"),
			new Placeholder("gpu", "client"), new Placeholder("client_mods", "client"),
			new Placeholder("rank", "rank"), new Placeholder("rank_name", "rank"), new Placeholder("has_rank", "rank"));

	private Snippets() {}

	@Nullable
	public static Snippet effect(String id) {
		for (Snippet s : EFFECTS) {
			if (s.id().equals(id)) {
				return s;
			}
		}
		return null;
	}

	/** Ejemplo que se ve a la derecha en las listas: "texto" pasa a una palabra de muestra. */
	public static String example(Snippet s, String sample) {
		return s.text().replace("texto que se desplaza", sample + " que se desplaza").replace("texto", sample);
	}

	/** Número sin ceros de más (1.50 → 1.5, 2.0 → 2). */
	public static String format(double v) {
		double r = Math.round(v * 1000) / 1000.0;
		return r == Math.rint(r) ? String.valueOf((long) r) : String.valueOf(r);
	}

	private static Snippet fx(String id, String category, String template, @Nullable String body, Param... params) {
		return new Snippet(id, category, template, body, List.of(params));
	}

	private static Param color(String key, String def) {
		return new Param(key, Kind.COLOR, def, 0, 0, 0, List.of());
	}

	private static Param num(String key, String def, double min, double max, double step) {
		return new Param(key, Kind.NUMBER, def, min, max, step, List.of());
	}

	private static Param text(String key, String def) {
		return new Param(key, Kind.TEXT, def, 0, 0, 0, List.of());
	}

	private static Param bool(String key, String def) {
		return new Param(key, Kind.BOOL, def, 0, 0, 0, List.of("true", "false"));
	}

	private static Param choice(String key, String def, String... options) {
		return new Param(key, Kind.CHOICE, def, 0, 0, 0, List.of(options));
	}
}
