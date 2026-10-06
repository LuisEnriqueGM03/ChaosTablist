package com.chaos.tablist.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.util.Mth;

/**
 * Toda la configuración del Tab. Se guarda tal cual en &lt;mundo&gt;/chaostablist/tablist.json y viaja por la red
 * como JSON (el cliente con mod la usa para dibujar y para el editor).
 */
public class TabConfig {

	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	public static final int MAX_TEMPLATE = 1024;
	public static final int MAX_LINES = 24;

	public boolean enabled = true;
	/** Cada cuántos ticks se recalcula el Tab de los clientes vanilla (las animaciones van a ese ritmo). */
	public int updateTicks = 2;
	public String serverName = "Chaos Server";
	/** Lo que ven los clientes sin mod en los placeholders que solo existen en el cliente ({fps}...). */
	public String clientOnlyFallback = "-";

	public List<String> header = new ArrayList<>(List.of(
			"",
			"<wave:1:0.6><gradient:#B57CF0:#3FD8EA:#B57CF0:speed=0.8><b>✦ {server_name} ✦</b></gradient></wave>",
			"<gray>Jugadores <white>{online}<dark_gray>/<gray>{max_players}  <dark_gray>| <gray>Tu ping <pc:{ping}>{ping}ms</pc>",
			""));
	public List<String> footer = new ArrayList<>(List.of(
			"",
			"<icon:tps:#55FF55> <gray>TPS <scale:{tps}:15:20:#FF5555:#FFFF55:#55FF55>{tps}</scale>  <icon:ram:#4AA3FF> <gray>RAM <bar:{ram_pct}:10:#55FF55:#3A3A3A:char=▌:to=#FF5555> <white>{ram}",
			"<icon:fps:#FFC300> <gray>FPS <white>{fps}  <icon:clock:#B57CF0> <gray>Hora <white>{local_time}",
			"{anim:footer}",
			""));

	/**
	 * Formato de cada fila. No sale en el editor: el rango de cada jugador lo pone Chaos Ranks ({rank}). Se puede
	 * cambiar a mano en el JSON.
	 */
	public RowFormat row = new RowFormat();

	/** Cambios por jugador (UUID → formato propio). */
	public Map<String, PlayerOverride> players = new LinkedHashMap<>();

	public Ping ping = new Ping();
	public Layout layout = new Layout();
	public Sorting sorting = new Sorting();
	public Map<String, Animation> animations = new LinkedHashMap<>(Map.of(
			"footer", Animation.of(2500,
					"<gray>Visita <gradient:#3FD8EA:#2F5BFF>discord.gg/chaos</gradient>",
					"<gray>Escribe <yellow>/help</yellow> si te pierdes",
					"<rainbow:1>¡Gracias por jugar!</rainbow>")));

	public static class RowFormat {
		public String prefix = "<if:{has_rank}:eq:true>{rank} </if>";
		public String name = "<white>{player}";
		public String suffix = "";
	}

	public static class PlayerOverride {
		public String name = "";
		@Nullable public String prefix;
		@Nullable public String format;
		@Nullable public String suffix;
		public boolean hidden;
	}

	public static class Ping {
		/** bars · number · both · hidden · text (solo la plantilla, sin barras). */
		public String style = "both";
		public String format = "<pc:{ping}>{ping}</pc><dark_gray>ms";
		/** Se añade al nombre que ven los clientes vanilla ("" = nada). */
		public String vanillaSuffix = " <dark_gray>[<pc:{ping}>{ping}ms</pc>]";
		public List<Threshold> thresholds = new ArrayList<>(List.of(
				new Threshold(60, "#55FF55"), new Threshold(120, "#A8FF3C"), new Threshold(200, "#FFFF55"),
				new Threshold(350, "#FFAA00"), new Threshold(Integer.MAX_VALUE, "#FF5555")));

		public int color(int ping) {
			if (ping < 0) {
				return 0xAAAAAA;
			}
			for (Threshold t : thresholds) {
				if (ping <= t.max) {
					return com.chaos.tablist.text.ColorUtil.parse(t.color, 0xFFFFFFFF) & 0xFFFFFF;
				}
			}
			return 0xFF5555;
		}
	}

	public static class Threshold {
		public int max;
		public String color;

		public Threshold(int max, String color) {
			this.max = max;
			this.color = color;
		}
	}

	/** Aspecto del Tab: solo lo usan los clientes con el mod. */
	public static class Layout {
		public String panelColor = "#C0100818";
		public String panelColor2 = "#C0201030";
		public String borderColor = "#FF9B36E8";
		public String borderColor2 = "#FF3FD8EA";
		public int borderWidth = 1;
		public boolean rounded = true;
		public boolean animatedBorder = true;
		public String rowColor = "#20FFFFFF";
		public String rowAltColor = "#30FFFFFF";
		public String selfRowColor = "#409B36E8";
		public String headerAlign = "center";
		public int rowHeight = 9;
		public int rowGap = 1;
		public int columnGap = 6;
		public int padding = 5;
		public int maxRows = 20;
		public int maxPlayers = 80;
		public int minColumnWidth = 90;
		public int topMargin = 10;
		public boolean showHeads = true;
		public boolean showInSingleplayer = true;
		public boolean spectatorsLast = true;
		public float spectatorAlpha = 0.55f;
		public boolean textShadow = true;
		/** none · fade · slide · scale · drop. */
		public String openAnimation = "drop";
		public int openMillis = 220;
		public float scale = 1;
		public String separatorColor = "#409B36E8";
		/** number · hearts · bar · hidden: cómo se pinta el marcador de la lista (si hay uno). */
		public String objectiveStyle = "number";
	}

	public static class Sorting {
		/** Claves en orden: rank, name, ping, gamemode, world, team. */
		public String order = "rank,name";
	}

	public static class Animation {
		public int intervalMs = 1000;
		public List<String> frames = new ArrayList<>();

		public static Animation of(int intervalMs, String... frames) {
			Animation a = new Animation();
			a.intervalMs = intervalMs;
			a.frames = new ArrayList<>(List.of(frames));
			return a;
		}

		@Nullable
		public String frame(double time) {
			if (frames.isEmpty()) {
				return null;
			}
			long i = (long) Math.floor(time * 1000 / Math.max(50, intervalMs));
			return frames.get((int) (i % frames.size()));
		}
	}

	// ---------------------------------------------------------------------------------------------

	/** Plantilla de la fila de un jugador: el formato general con lo que tenga cambiado a mano. */
	public String rowTemplate(@Nullable String uuid) {
		String prefix = row.prefix;
		String name = row.name;
		String suffix = row.suffix;
		PlayerOverride o = uuid == null ? null : players.get(uuid);
		if (o != null) {
			prefix = o.prefix != null ? o.prefix : prefix;
			name = o.format != null ? o.format : name;
			suffix = o.suffix != null ? o.suffix : suffix;
		}
		return prefix + name + suffix;
	}

	public boolean hidden(@Nullable String uuid) {
		PlayerOverride o = uuid == null ? null : players.get(uuid);
		return o != null && o.hidden;
	}

	@Nullable
	public String animationFrame(String name, double time) {
		Animation a = animations.get(name);
		return a == null ? null : a.frame(time);
	}

	public String toJson() {
		return GSON.toJson(this);
	}

	public static TabConfig fromJson(String json) {
		TabConfig c = GSON.fromJson(json, TabConfig.class);
		if (c == null) {
			c = new TabConfig();
		}
		c.sanitize();
		return c;
	}

	public TabConfig copy() {
		return fromJson(toJson());
	}

	/** Deja todo dentro de lo permitido (lo que llega del cliente o de un JSON editado a mano no es de fiar). */
	public void sanitize() {
		updateTicks = Mth.clamp(updateTicks, 1, 200);
		serverName = clip(serverName, 64);
		clientOnlyFallback = clip(clientOnlyFallback, 16);
		header = lines(header);
		footer = lines(footer);
		if (row == null) {
			row = new RowFormat();
		}
		row.prefix = clip(row.prefix, MAX_TEMPLATE);
		row.name = clip(row.name, MAX_TEMPLATE);
		row.suffix = clip(row.suffix, MAX_TEMPLATE);
		if (players == null) {
			players = new LinkedHashMap<>();
		}
		players.values().removeIf(p -> p == null);
		for (PlayerOverride p : players.values()) {
			p.name = clip(p.name, 16);
			p.prefix = p.prefix == null ? null : clip(p.prefix, MAX_TEMPLATE);
			p.format = p.format == null ? null : clip(p.format, MAX_TEMPLATE);
			p.suffix = p.suffix == null ? null : clip(p.suffix, MAX_TEMPLATE);
		}
		if (ping == null) {
			ping = new Ping();
		}
		ping.style = clip(ping.style, 16);
		ping.format = clip(ping.format, MAX_TEMPLATE);
		ping.vanillaSuffix = clip(ping.vanillaSuffix, MAX_TEMPLATE);
		if (ping.thresholds == null || ping.thresholds.isEmpty()) {
			ping.thresholds = new Ping().thresholds;
		}
		ping.thresholds.removeIf(t -> t == null || t.color == null);
		ping.thresholds.sort((a, b) -> Integer.compare(a.max, b.max));
		if (layout == null) {
			layout = new Layout();
		}
		Layout l = layout;
		l.rowHeight = Mth.clamp(l.rowHeight, 8, 24);
		l.rowGap = Mth.clamp(l.rowGap, 0, 8);
		l.columnGap = Mth.clamp(l.columnGap, 0, 40);
		l.padding = Mth.clamp(l.padding, 0, 30);
		l.maxRows = Mth.clamp(l.maxRows, 1, 60);
		l.maxPlayers = Mth.clamp(l.maxPlayers, 1, 500);
		l.minColumnWidth = Mth.clamp(l.minColumnWidth, 20, 400);
		l.topMargin = Mth.clamp(l.topMargin, 0, 200);
		l.borderWidth = Mth.clamp(l.borderWidth, 0, 4);
		l.openMillis = Mth.clamp(l.openMillis, 0, 2000);
		l.scale = Mth.clamp(l.scale, 0.5f, 2f);
		l.spectatorAlpha = Mth.clamp(l.spectatorAlpha, 0.1f, 1f);
		if (sorting == null) {
			sorting = new Sorting();
		}
		sorting.order = clip(sorting.order, 128);
		// "group" venía de cuando había grupos.
		List<String> order = new ArrayList<>(List.of(sorting.order.split(",")));
		order.replaceAll(String::trim);
		order.removeIf(k -> k.isEmpty() || k.equalsIgnoreCase("group"));
		sorting.order = String.join(",", order);
		if (animations == null) {
			animations = new LinkedHashMap<>();
		}
		animations.values().removeIf(a -> a == null);
		for (Animation a : animations.values()) {
			a.intervalMs = Mth.clamp(a.intervalMs, 50, 60_000);
			a.frames = a.frames == null ? new ArrayList<>() : a.frames;
			a.frames.replaceAll(f -> clip(f, MAX_TEMPLATE));
			if (a.frames.size() > 64) {
				a.frames = new ArrayList<>(a.frames.subList(0, 64));
			}
		}
	}

	private static List<String> lines(@Nullable List<String> in) {
		List<String> out = new ArrayList<>();
		if (in != null) {
			for (String s : in) {
				if (out.size() >= MAX_LINES) {
					break;
				}
				out.add(clip(s, MAX_TEMPLATE));
			}
		}
		return out;
	}

	private static String clip(@Nullable String s, int max) {
		if (s == null) {
			return "";
		}
		return s.length() > max ? s.substring(0, max) : s;
	}
}
