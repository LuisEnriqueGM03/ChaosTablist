package com.chaos.tablist.client.editor;

import java.util.List;

/**
 * Lo que se puede insertar desde el editor: efectos y placeholders, agrupados por categoría (con su clave de
 * traducción). En los efectos, "texto" es lo que se sustituye por la selección al insertar.
 */
public final class Snippets {

	public record Snippet(String id, String category, String text) {}

	public record Placeholder(String key, String category) {}

	public static final List<Snippet> EFFECTS = List.of(
			new Snippet("gradient", "color", "<gradient:#FF5555:#5555FF>texto</gradient>"),
			new Snippet("gradient_anim", "color", "<gradient:#B57CF0:#3FD8EA:speed=1>texto</gradient>"),
			new Snippet("rainbow", "color", "<rainbow:1>texto</rainbow>"),
			new Snippet("fade", "color", "<fade:#FF5555:#FFFF55:1>texto</fade>"),
			new Snippet("pulse", "color", "<pulse:#FF3B3B:1>texto</pulse>"),
			new Snippet("sparkle", "color", "<sparkle:#FFFFFF>texto</sparkle>"),
			new Snippet("wave", "motion", "<wave>texto</wave>"),
			new Snippet("bounce", "motion", "<bounce>texto</bounce>"),
			new Snippet("shake", "motion", "<shake>texto</shake>"),
			new Snippet("fadein", "motion", "<fadein:0.4:0.05>texto</fadein>"),
			new Snippet("typewriter", "animated", "<typewriter:8:2:cursor=_>texto</typewriter>"),
			new Snippet("scroll", "animated", "<scroll:10:4>texto que se desplaza</scroll>"),
			new Snippet("blink", "animated", "<blink>texto</blink>"),
			new Snippet("cycle", "animated", "<cycle:1.5:fade=true>uno<next>dos<next>tres</cycle>"),
			new Snippet("bold", "style", "<b>texto</b>"),
			new Snippet("italic", "style", "<i>texto</i>"),
			new Snippet("underlined", "style", "<u>texto</u>"),
			new Snippet("strikethrough", "style", "<st>texto</st>"),
			new Snippet("obfuscated", "style", "<obf>texto</obf>"),
			new Snippet("smallcaps", "style", "<smallcaps>texto</smallcaps>"),
			new Snippet("upper", "style", "<upper>texto</upper>"),
			new Snippet("bg", "style", "<bg:#C0000000:2>texto</bg>"),
			new Snippet("bg_border", "style", "<bg:#C02A1606:2:true:#FFE8B23A>texto</bg>"),
			new Snippet("outline", "style", "<outline:#000000>texto</outline>"),
			new Snippet("glow", "style", "<glow:#FFD700>texto</glow>"),
			new Snippet("shadow", "style", "<shadow:#5A0F8C>texto</shadow>"),
			new Snippet("noshadow", "style", "<noshadow>texto</noshadow>"),
			new Snippet("alpha", "style", "<alpha:0.5>texto</alpha>"),
			new Snippet("font", "style", "<font:minecraft:uniform>texto</font>"),
			new Snippet("bar", "elements", "<bar:{ram_pct}:10:#55FF55:#3A3A3A:char=▌:to=#FF5555>"),
			new Snippet("badge", "elements", "<badge:EVENTO:#7B2CBF:#FFFFFF:star:#3B1A5C>"),
			new Snippet("head", "elements", "<head:{player}>"),
			new Snippet("item", "elements", "<item:minecraft:diamond_sword>"),
			new Snippet("image", "elements", "<icon:custom/logo>"),
			new Snippet("image_big", "elements", "<img:logo:64:24>"),
			new Snippet("space", "elements", "<space:6>"),
			new Snippet("br", "elements", "<br>"),
			new Snippet("left", "elements", "<left>"),
			new Snippet("center", "elements", "<center>"),
			new Snippet("right", "elements", "<right>"),
			new Snippet("pc", "logic", "<pc:{ping}>{ping}ms</pc>"),
			new Snippet("scale", "logic", "<scale:{tps}:15:20:#FF5555:#FFFF55:#55FF55>{tps}</scale>"),
			new Snippet("if", "logic", "<if:{op}:eq:true>admin<else>jugador</if>"),
			new Snippet("legacy", "logic", "&a&lverde &r&#FF55FFrosa"));

	public static final List<Placeholder> PLACEHOLDERS = List.of(
			new Placeholder("server_name", "server"), new Placeholder("online", "server"),
			new Placeholder("max_players", "server"), new Placeholder("tps", "server"), new Placeholder("mspt", "server"),
			new Placeholder("ram", "server"), new Placeholder("ram_pct", "server"), new Placeholder("ram_used", "server"),
			new Placeholder("ram_max", "server"), new Placeholder("cpu", "server"), new Placeholder("uptime", "server"),
			new Placeholder("date", "server"), new Placeholder("time", "server"), new Placeholder("day", "server"),
			new Placeholder("world_time", "server"), new Placeholder("weather", "server"),
			new Placeholder("weather_icon", "server"), new Placeholder("motd", "server"), new Placeholder("version", "server"),
			new Placeholder("player", "player"), new Placeholder("ping", "player"),
			new Placeholder("world", "player"), new Placeholder("dimension", "player"), new Placeholder("biome", "player"),
			new Placeholder("x", "player"), new Placeholder("y", "player"), new Placeholder("z", "player"),
			new Placeholder("health", "player"), new Placeholder("max_health", "player"), new Placeholder("food", "player"),
			new Placeholder("level", "player"), new Placeholder("gamemode", "player"), new Placeholder("deaths", "player"),
			new Placeholder("kills", "player"), new Placeholder("mob_kills", "player"), new Placeholder("playtime", "player"),
			new Placeholder("afk", "player"), new Placeholder("op", "player"), new Placeholder("team", "player"),
			new Placeholder("fps", "client"), new Placeholder("client_ram", "client"),
			new Placeholder("client_ram_pct", "client"), new Placeholder("my_ping", "client"),
			new Placeholder("local_time", "client"), new Placeholder("local_time_s", "client"),
			new Placeholder("local_date", "client"),
			new Placeholder("rank", "rank"), new Placeholder("rank_name", "rank"), new Placeholder("has_rank", "rank"));

	private Snippets() {}

	/** Ejemplo que se ve a la derecha en las listas: "texto" pasa a una palabra de muestra. */
	public static String example(Snippet s, String sample) {
		return s.text().replace("texto que se desplaza", sample + " que se desplaza").replace("texto", sample);
	}
}
