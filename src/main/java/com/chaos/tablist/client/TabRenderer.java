package com.chaos.tablist.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.chaos.tablist.config.TabConfig;
import com.chaos.tablist.text.ColorUtil;
import com.chaos.tablist.text.Evaluator;
import com.chaos.tablist.text.Glyph;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/**
 * El Tab del cliente con mod: lo monta entero a cada frame con la configuración del servidor, así que las
 * animaciones van a los FPS del juego. El editor usa el mismo código para la vista previa.
 */
public final class TabRenderer {

	/** Un jugador de la lista (de verdad o de ejemplo en la vista previa). */
	public record Entry(UUID id, String name, PlayerSkin skin, GameType mode, int latency, Map<String, String> values,
			boolean self, @Nullable PlayerInfo info) {}

	private static final ResourceLocation[] PING = {
			ResourceLocation.withDefaultNamespace("icon/ping_unknown"),
			ResourceLocation.withDefaultNamespace("icon/ping_1"),
			ResourceLocation.withDefaultNamespace("icon/ping_2"),
			ResourceLocation.withDefaultNamespace("icon/ping_3"),
			ResourceLocation.withDefaultNamespace("icon/ping_4"),
			ResourceLocation.withDefaultNamespace("icon/ping_5")};

	public static final int MAX_FAKES = 20;
	private static final String[] FAKE_NAMES = {"Steve", "Alex", "Notch", "Herobrine", "Jeb_", "Dinnerbone", "Grumm",
			"Kirby", "Luna", "Max", "Sofia", "Diego", "Valen", "Nico", "Zoe", "Bruno", "Mia", "Leo", "Iris", "Hugo"};
	private static final int[] FAKE_PINGS = {12, 35, 64, 98, 142, 210, 330, 480, 760, 1200};

	/** Jugadores de ejemplo que se añaden al Tab real (solo la autoprueba). */
	public static int testFakes;

	private static boolean wasDown;
	private static float progress;
	private static long lastFrame;
	private static double openedAt;

	private TabRenderer() {}

	// ---------------------------------------------------------------------------------------------
	// HUD

	/** Llamado en lugar del Tab vanilla. false = no hay configuración del servidor (se pinta el vanilla). */
	public static boolean renderHud(Minecraft mc, GuiGraphics g) {
		TabConfig cfg = ClientState.config();
		if (cfg == null || !cfg.enabled || mc.level == null || mc.player == null || mc.getConnection() == null) {
			return false;
		}
		PlayerTabOverlay overlay = mc.gui.getTabList();
		Scoreboard board = mc.level.getScoreboard();
		Objective objective = board.getDisplayObjective(DisplaySlot.LIST);
		boolean down = mc.options.keyPlayerList.isDown();
		boolean allowed = !mc.isLocalServer() || cfg.layout.showInSingleplayer
				|| mc.getConnection().getListedOnlinePlayers().size() > 1 || objective != null;
		boolean show = down && allowed;
		if (show && !wasDown) {
			openedAt = ClientContext.now();
		}
		wasDown = show;
		overlay.setVisible(show);

		long now = Util.getMillis();
		float dt = lastFrame == 0 ? 0 : (now - lastFrame) / Math.max(1f, cfg.layout.openMillis);
		lastFrame = now;
		progress = Mth.clamp(progress + (show ? dt : -dt), 0, 1);
		if (cfg.layout.openAnimation.equals("none") || cfg.layout.openMillis == 0) {
			progress = show ? 1 : 0;
		}
		if (progress <= 0) {
			return true;
		}
		render(g, mc.font, cfg, entries(mc, cfg), ClientState.global(), g.guiWidth() / 2, cfg.layout.topMargin,
				g.guiWidth() - 20, g.guiHeight() - cfg.layout.topMargin - 10, objective, board, ClientContext.now(),
				openedAt, progress, false);
		return true;
	}

	/** Los jugadores conectados, ordenados según la configuración. */
	public static List<Entry> entries(Minecraft mc, TabConfig cfg) {
		List<Entry> list = new ArrayList<>();
		UUID self = mc.player == null ? null : mc.player.getUUID();
		for (PlayerInfo info : mc.getConnection().getListedOnlinePlayers()) {
			UUID id = info.getProfile().getId();
			if (cfg.hidden(id.toString())) {
				continue;
			}
			Map<String, String> values = new HashMap<>(ClientState.player(id));
			values.putIfAbsent("player", info.getProfile().getName());
			values.put("ping", String.valueOf(info.getLatency()));
			values.putIfAbsent("gamemode", info.getGameMode().getName());
			list.add(new Entry(id, info.getProfile().getName(), info.getSkin(), info.getGameMode(), info.getLatency(),
					values, id.equals(self), info));
		}
		if (testFakes > 0) {
			list.addAll(fakes(cfg, testFakes));
		}
		list.sort(comparator(cfg));
		if (list.size() > cfg.layout.maxPlayers) {
			list = new ArrayList<>(list.subList(0, cfg.layout.maxPlayers));
		}
		return list;
	}

	/** Jugadores de ejemplo para la vista previa: cada uno en un grupo, con pings y modos variados. */
	public static List<Entry> fakes(TabConfig cfg, int count) {
		List<Entry> list = new ArrayList<>();
		for (int i = 0; i < Math.min(count, MAX_FAKES); i++) {
			String name = FAKE_NAMES[i];
			UUID id = UUID.nameUUIDFromBytes(("chaostablist:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
			Map<String, String> v = new HashMap<>();
			TabConfig.GroupDef group = cfg.groups.isEmpty() ? null : cfg.groups.get(i % cfg.groups.size());
			int ping = FAKE_PINGS[i % FAKE_PINGS.length];
			GameType mode = i == 5 ? GameType.SPECTATOR : i == 3 ? GameType.CREATIVE : GameType.SURVIVAL;
			v.put("player", name);
			v.put("ping", String.valueOf(ping));
			v.put("group", group == null ? "default" : group.id);
			v.put("group_priority", String.valueOf(group == null ? 0 : group.priority));
			v.put("has_rank", "false");
			v.put("world", i % 3 == 0 ? "overworld" : i % 3 == 1 ? "the_nether" : "the_end");
			v.put("gamemode", mode.getName());
			v.put("health", String.valueOf(20 - i));
			v.put("level", String.valueOf(i * 7));
			v.put("op", String.valueOf(group != null && group.condition.contains("op")));
			list.add(new Entry(id, name, net.minecraft.client.resources.DefaultPlayerSkin.get(id), mode, ping, v, false, null));
		}
		return list;
	}

	public static Comparator<Entry> comparator(TabConfig cfg) {
		Comparator<Entry> c = Comparator.comparingInt(e -> cfg.layout.spectatorsLast && e.mode() == GameType.SPECTATOR ? 1 : 0);
		for (String key : cfg.sorting.order.split(",")) {
			c = switch (key.trim().toLowerCase(Locale.ROOT)) {
				case "group" -> c.thenComparing(e -> -num(e.values().get("group_priority")));
				case "rank" -> c.thenComparing(e -> -num(e.values().get("rank_priority")));
				case "name" -> c.thenComparing(Entry::name, String::compareToIgnoreCase);
				case "ping" -> c.thenComparingInt(Entry::latency);
				case "gamemode" -> c.thenComparingInt(e -> e.mode().getId());
				case "world" -> c.thenComparing(e -> e.values().getOrDefault("world", ""));
				case "team" -> c.thenComparing(e -> {
					PlayerTeam team = e.info() == null ? null : e.info().getTeam();
					return team == null ? "" : team.getName();
				});
				default -> c;
			};
		}
		return c.thenComparing(Entry::name, String::compareToIgnoreCase);
	}

	private static double num(@Nullable String s) {
		try {
			return s == null ? 0 : Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	// ---------------------------------------------------------------------------------------------
	// Dibujo

	private record Line(List<Glyph> glyphs, int width, int height, int align) {}

	private record Row(Entry entry, List<Glyph> name, int nameWidth, @Nullable Component score, int scoreWidth,
			List<Glyph> ping, int pingWidth) {}

	/**
	 * Dibuja el Tab completo centrado en {@code centerX} desde {@code top}.
	 *
	 * @param progress 0..1 de la animación de apertura
	 * @param vanillaLook simular lo que ve un cliente sin el mod (vista previa del editor)
	 */
	public static void render(GuiGraphics g, Font font, TabConfig cfg, List<Entry> entries, Map<String, String> global,
			int centerX, int top, int maxWidth, int maxHeight, @Nullable Objective objective, @Nullable Scoreboard board, double time,
			double openedAt, float progress, boolean vanillaLook) {
		if (vanillaLook) {
			cfg = vanillaLayout(cfg);
		}
		TabConfig.Layout l = cfg.layout;
		Map<String, String> selfValues = Map.of();
		for (Entry e : entries) {
			if (e.self()) {
				selfValues = e.values();
			}
		}
		ClientContext selfCtx = new ClientContext(cfg, selfValues, global, time, openedAt, vanillaLook);

		List<Line> header = lines(font, cfg.header, selfCtx, l.headerAlign);
		List<Line> footer = lines(font, cfg.footer, selfCtx, l.headerAlign);
		trim(header);
		trim(footer);

		// Filas.
		boolean heads = l.showHeads;
		String pingStyle = cfg.ping.style.toLowerCase(Locale.ROOT);
		boolean bars = pingStyle.equals("bars") || pingStyle.equals("both");
		boolean number = pingStyle.equals("number") || pingStyle.equals("both") || pingStyle.equals("text");
		List<Row> rows = new ArrayList<>();
		int nameMax = 0;
		int scoreMax = 0;
		int pingMax = 0;
		boolean hearts = objective != null && objective.getRenderType() == ObjectiveCriteria.RenderType.HEARTS;
		for (Entry e : entries) {
			ClientContext ctx = new ClientContext(cfg, e.values(), global, time, openedAt, vanillaLook);
			String template = cfg.rowTemplate(e.values().get("group"), e.id().toString());
			List<Glyph> name = vanillaLook ? vanillaize(Evaluator.eval(template + cfg.ping.vanillaSuffix, ctx))
					: Evaluator.eval(template, ctx);
			if (e.mode() == GameType.SPECTATOR) {
				for (Glyph gl : name) {
					gl.alpha *= l.spectatorAlpha;
					gl.italic = gl.italic == null ? Boolean.TRUE : gl.italic;
				}
			}
			Component score = null;
			if (objective != null && board != null && !l.objectiveStyle.equals("hidden") && e.mode() != GameType.SPECTATOR) {
				ReadOnlyScoreInfo info = board.getPlayerScoreInfo(ScoreHolder.forNameOnly(e.name()), objective);
				int value = info == null ? 0 : info.value();
				if (hearts || l.objectiveStyle.equals("hearts")) {
					score = Component.literal("❤ " + (hearts ? String.format(Locale.ROOT, "%.1f", value / 2f) : value))
							.withColor(0xFF5555);
				} else {
					score = ReadOnlyScoreInfo.safeFormatValue(info, objective.numberFormatOrDefault(StyledFormat.PLAYER_LIST_DEFAULT));
				}
			}
			List<Glyph> ping = number ? Evaluator.eval(cfg.ping.format, ctx) : List.of();
			int pw = number ? RichText.width(font, ping) : 0;
			if (bars) {
				pw += (number ? 2 : 0) + 10;
			}
			int nw = RichText.width(font, name);
			int sw = score == null ? 0 : font.width(score);
			rows.add(new Row(e, name, nw, score, sw, ping, pw));
			nameMax = Math.max(nameMax, nw);
			scoreMax = Math.max(scoreMax, sw);
			pingMax = Math.max(pingMax, pw);
		}

		int n = rows.size();
		int cols = 1;
		int perCol = n;
		while (perCol > l.maxRows) {
			cols++;
			perCol = (n + cols - 1) / cols;
		}
		int colW = Math.max(l.minColumnWidth, 2 + (heads ? 10 : 0) + nameMax + (scoreMax > 0 ? 6 + scoreMax : 0)
				+ (pingMax > 0 ? 6 + pingMax : 0) + 2);
		int listW = n == 0 ? 0 : cols * colW + (cols - 1) * l.columnGap;
		int inner = listW;
		for (Line line : header) {
			inner = Math.max(inner, line.width());
		}
		for (Line line : footer) {
			inner = Math.max(inner, line.width());
		}
		int pad = l.padding;
		int width = inner + pad * 2;
		int step = l.rowHeight + l.rowGap;
		int headerH = header.stream().mapToInt(Line::height).sum();
		int footerH = footer.stream().mapToInt(Line::height).sum();
		int listH = perCol * step - (perCol > 0 ? l.rowGap : 0);
		int gap = 4;
		int height = pad + headerH + (headerH > 0 && n > 0 ? gap : 0) + listH + (footerH > 0 && n > 0 ? gap : 0) + footerH + pad;

		// Animación de apertura.
		float e = ease(progress);
		float alpha = 1;
		float scale = l.scale;
		float offsetY = 0;
		switch (l.openAnimation) {
			case "fade" -> alpha = e;
			case "slide" -> {
				offsetY = -(1 - e) * (height + top);
				alpha = Mth.clamp(progress * 2, 0, 1);
			}
			case "scale" -> {
				scale *= 0.6f + 0.4f * e;
				alpha = e;
			}
			case "drop" -> {
				offsetY = -(1 - easeOutBack(progress)) * 24;
				alpha = e;
			}
			default -> {}
		}
		// Si no cabe, se encoge (nunca se agranda por esto).
		if (width * scale > maxWidth && width > 0) {
			scale = maxWidth / (float) width;
		}
		if (maxHeight > 0 && height * scale > maxHeight && height > 0) {
			scale = maxHeight / (float) height;
		}

		g.pose().pushPose();
		g.pose().translate(centerX, top + offsetY, 0);
		g.pose().scale(scale, scale, 1);
		int x0 = -width / 2;
		int x1 = x0 + width;

		RenderSystem.enableBlend();
		panel(g, cfg, x0, 0, x1, height, alpha, time);

		int y = pad;
		for (Line line : header) {
			drawLine(g, font, line, x0 + pad, inner, y, alpha, l.textShadow);
			y += line.height();
		}
		if (headerH > 0 && n > 0) {
			y += gap / 2;
			int sep = ColorUtil.parse(l.separatorColor, 0);
			if ((sep >>> 24) > 0) {
				g.fill(x0 + pad, y - 1, x1 - pad, y, RichText.scaleAlpha(sep, alpha));
			}
			y += gap - gap / 2;
		}

		int listX = x0 + pad + (inner - listW) / 2;
		int rowColor = ColorUtil.parse(l.rowColor, 0);
		int altColor = ColorUtil.parse(l.rowAltColor, rowColor);
		int selfColor = ColorUtil.parse(l.selfRowColor, rowColor);
		for (int i = 0; i < n; i++) {
			Row row = rows.get(i);
			int col = i / perCol;
			int r = i % perCol;
			int rx = listX + col * (colW + l.columnGap);
			int ry = y + r * step;
			int bg = row.entry().self() ? selfColor : r % 2 == 1 ? altColor : rowColor;
			if ((bg >>> 24) > 0) {
				g.fill(rx, ry, rx + colW, ry + l.rowHeight, RichText.scaleAlpha(bg, alpha));
			}
			int ty = ry + (l.rowHeight - 8 + 1) / 2;
			int cx = rx + 1;
			if (heads) {
				RenderSystem.setShaderColor(1, 1, 1, alpha * (row.entry().mode() == GameType.SPECTATOR ? l.spectatorAlpha : 1));
				boolean hat = true;
				boolean upsideDown = false;
				Minecraft mc = Minecraft.getInstance();
				if (mc.level != null) {
					Player player = mc.level.getPlayerByUUID(row.entry().id());
					hat = player == null || player.isModelPartShown(PlayerModelPart.HAT);
					upsideDown = player != null && net.minecraft.client.renderer.entity.LivingEntityRenderer.isEntityUpsideDown(player);
				}
				PlayerFaceRenderer.draw(g, row.entry().skin().texture(), cx, ty, 8, hat, upsideDown);
				RenderSystem.setShaderColor(1, 1, 1, 1);
				cx += 10;
			}
			RichText.draw(g, font, row.name(), cx, ty, alpha, l.textShadow);

			int right = rx + colW - 2;
			if (row.pingWidth() > 0) {
				if (bars) {
					g.pose().pushPose();
					g.pose().translate(0, 0, 100);
					RenderSystem.setShaderColor(1, 1, 1, alpha);
					g.blitSprite(pingSprite(row.entry().latency()), right - 10, ty, 10, 8);
					RenderSystem.setShaderColor(1, 1, 1, 1);
					g.pose().popPose();
				}
				if (number) {
					int w = RichText.width(font, row.ping());
					RichText.draw(g, font, row.ping(), right - (bars ? 12 : 0) - w, ty, alpha, l.textShadow);
				}
				right -= pingMax + 6;
			}
			if (row.score() != null) {
				g.drawString(font, row.score(), right - row.scoreWidth(), ty, RichText.scaleAlpha(0xFFFFFFFF, alpha), l.textShadow);
			}
		}
		y += listH;
		if (footerH > 0 && n > 0) {
			y += gap;
		}
		for (Line line : footer) {
			drawLine(g, font, line, x0 + pad, inner, y, alpha, l.textShadow);
			y += line.height();
		}
		g.pose().popPose();
	}

	private static List<Line> lines(Font font, List<String> templates, ClientContext ctx, String defaultAlign) {
		int def = switch (defaultAlign) {
			case "left" -> Evaluator.Line.LEFT;
			case "right" -> Evaluator.Line.RIGHT;
			default -> Evaluator.Line.CENTER;
		};
		List<Line> out = new ArrayList<>();
		for (String template : templates) {
			Evaluator.Line line = Evaluator.line(template, ctx);
			int align = line.align() == Evaluator.Line.UNSET ? def : line.align();
			List<Glyph> glyphs = ctx.vanilla() ? vanillaize(line.glyphs()) : line.glyphs();
			for (List<Glyph> part : RichText.lines(glyphs)) {
				out.add(new Line(part, RichText.width(font, part), RichText.height(part), align));
			}
		}
		return out;
	}

	/** Las líneas vacías del principio y del final solo dejan un poco de aire, no una fila entera. */
	private static void trim(List<Line> lines) {
		while (!lines.isEmpty() && lines.get(0).glyphs().isEmpty()) {
			lines.remove(0);
		}
		while (!lines.isEmpty() && lines.get(lines.size() - 1).glyphs().isEmpty()) {
			lines.remove(lines.size() - 1);
		}
	}

	private static void drawLine(GuiGraphics g, Font font, Line line, int x, int width, int y, float alpha, boolean shadow) {
		int lx = switch (line.align()) {
			case Evaluator.Line.LEFT -> x;
			case Evaluator.Line.RIGHT -> x + width - line.width();
			default -> x + (width - line.width()) / 2;
		};
		RichText.draw(g, font, line.glyphs(), lx, y + line.height() - 9, alpha, shadow);
	}

	private static void panel(GuiGraphics g, TabConfig cfg, int x0, int y0, int x1, int y1, float alpha, double time) {
		TabConfig.Layout l = cfg.layout;
		int top = RichText.scaleAlpha(ColorUtil.parse(l.panelColor, 0x80000000), alpha);
		int bottom = RichText.scaleAlpha(ColorUtil.parse(l.panelColor2, ColorUtil.parse(l.panelColor, 0x80000000)), alpha);
		int r = l.rounded ? 2 : 0;
		if (r > 0) {
			// Esquinas redondeadas de 2 px: dos filas recortadas arriba y abajo.
			g.fill(x0 + 2, y0, x1 - 2, y0 + 1, top);
			g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, top);
			g.fillGradient(x0, y0 + 2, x1, y1 - 2, top, bottom);
			g.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, bottom);
			g.fill(x0 + 2, y1 - 1, x1 - 2, y1, bottom);
		} else {
			g.fillGradient(x0, y0, x1, y1, top, bottom);
		}
		int bw = l.borderWidth;
		if (bw <= 0) {
			return;
		}
		int c1 = ColorUtil.parse(l.borderColor, 0);
		int c2 = ColorUtil.parse(l.borderColor2, c1);
		if (((c1 | c2) >>> 24) == 0) {
			return;
		}
		int[] stops = {c1, c2};
		float shift = l.animatedBorder ? ColorUtil.frac(time * 0.15) : 0;
		int w = x1 - x0;
		int h = y1 - y0;
		float perimeter = 2f * (w + h);
		int seg = 4;
		// Borde recorrido en tramos de 4 px con un degradado que da vueltas.
		for (int i = r; i < w - r; i += seg) {
			int len = Math.min(seg, w - r - i);
			g.fill(x0 + i, y0, x0 + i + len, y0 + bw, RichText.scaleAlpha(ColorUtil.stops(stops, i / perimeter - shift, true), alpha));
			g.fill(x0 + i, y1 - bw, x0 + i + len, y1,
					RichText.scaleAlpha(ColorUtil.stops(stops, (w + h + (w - i)) / perimeter - shift, true), alpha));
		}
		for (int j = r; j < h - r; j += seg) {
			int len = Math.min(seg, h - r - j);
			g.fill(x1 - bw, y0 + j, x1, y0 + j + len, RichText.scaleAlpha(ColorUtil.stops(stops, (w + j) / perimeter - shift, true), alpha));
			g.fill(x0, y0 + j, x0 + bw, y0 + j + len,
					RichText.scaleAlpha(ColorUtil.stops(stops, (2 * w + h + (h - j)) / perimeter - shift, true), alpha));
		}
		if (r > 0) {
			int corner = RichText.scaleAlpha(ColorUtil.stops(stops, -shift, true), alpha);
			g.fill(x0 + 1, y0 + 1, x0 + 2, y0 + 2, corner);
			g.fill(x1 - 2, y0 + 1, x1 - 1, y0 + 2, RichText.scaleAlpha(ColorUtil.stops(stops, w / perimeter - shift, true), alpha));
			g.fill(x1 - 2, y1 - 2, x1 - 1, y1 - 1, RichText.scaleAlpha(ColorUtil.stops(stops, (w + h) / perimeter - shift, true), alpha));
			g.fill(x0 + 1, y1 - 2, x0 + 2, y1 - 1, RichText.scaleAlpha(ColorUtil.stops(stops, (2 * w + h) / perimeter - shift, true), alpha));
		}
	}

	/** Lo mismo que vería un cliente vanilla: símbolos de respaldo y sin efectos del cliente. */
	private static List<Glyph> vanillaize(List<Glyph> glyphs) {
		List<Glyph> out = new ArrayList<>(glyphs.size());
		for (Glyph gl : glyphs) {
			if (gl.hidden) {
				continue;
			}
			Glyph c = gl.copy();
			if (c.fallback != null || c.kind != Glyph.Kind.TEXT) {
				c.text = c.fallback == null ? "" : c.fallback;
				c.kind = Glyph.Kind.TEXT;
				c.fallback = null;
				c.font = null;
			}
			c.alpha = 1;
			c.dx = 0;
			c.dy = 0;
			c.shadow = Glyph.UNSET;
			c.outline = Glyph.UNSET;
			c.glow = Glyph.UNSET;
			c.backgrounds.clear();
			out.add(c);
		}
		return out;
	}

	private static TabConfig vanillaCache;
	private static TabConfig vanillaSource;

	/** El editor cambia la configuración en el sitio: la copia "vanilla" se tiene que rehacer. */
	public static void invalidate() {
		vanillaSource = null;
	}

	/** El Tab vanilla: cajas negras translúcidas, filas grises, barras de ping y sin animación. */
	private static TabConfig vanillaLayout(TabConfig cfg) {
		if (vanillaSource != cfg) {
			TabConfig v = cfg.copy();
			TabConfig.Layout l = v.layout;
			l.panelColor = "#80000000";
			l.panelColor2 = "#80000000";
			l.borderWidth = 0;
			l.rounded = false;
			l.rowColor = "#20FFFFFF";
			l.rowAltColor = "#20FFFFFF";
			l.selfRowColor = "#20FFFFFF";
			l.separatorColor = "#00000000";
			l.rowHeight = 8;
			l.rowGap = 1;
			l.padding = 1;
			l.minColumnWidth = 20;
			l.headerAlign = "center";
			l.openAnimation = "none";
			l.textShadow = true;
			l.scale = 1;
			v.ping.style = "bars";
			vanillaCache = v;
			vanillaSource = cfg;
		}
		return vanillaCache;
	}

	private static ResourceLocation pingSprite(int latency) {
		if (latency < 0) {
			return PING[0];
		}
		if (latency < 150) {
			return PING[5];
		}
		if (latency < 300) {
			return PING[4];
		}
		if (latency < 600) {
			return PING[3];
		}
		return latency < 1000 ? PING[2] : PING[1];
	}

	private static float ease(float t) {
		return 1 - (1 - t) * (1 - t) * (1 - t);
	}

	private static float easeOutBack(float t) {
		float c1 = 1.70158f;
		float c3 = c1 + 1;
		return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
	}
}
