package com.chaos.tablist.client.editor;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.chaos.tablist.ChaosTablist;
import com.chaos.tablist.Lang;
import com.chaos.tablist.client.ClientContext;
import com.chaos.tablist.client.ClientState;
import com.chaos.tablist.client.CustomIcons;
import com.chaos.tablist.client.RichText;
import com.chaos.tablist.client.TabRenderer;
import com.chaos.tablist.config.TabConfig;
import com.chaos.tablist.mixin.client.EditBoxAccessor;
import com.chaos.tablist.mixin.client.MultiLineEditBoxAccessor;
import com.chaos.tablist.net.TabPayloads;
import com.chaos.tablist.text.Badges;
import com.chaos.tablist.text.ColorUtil;
import com.chaos.tablist.text.Evaluator;
import com.chaos.tablist.text.Glyph;
import com.chaos.tablist.text.Icons;
import com.chaos.tablist.text.Palette;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.scores.DisplaySlot;

/**
 * Editor del Tab (/tablist): pestañas a la izquierda y la vista previa en vivo a la derecha, con jugadores
 * reales y de ejemplo. "Guardar" lo aplica al momento a todo el servidor (o se aplica solo al escribir si
 * "Auto" está activado). Pensado para caber en pantallas pequeñas (GUI de ~427x240): los formularios son
 * filas con scroll. Texturas propias (tools/gen_gui.py).
 */
public class TablistEditorScreen extends Screen {

	private enum Tab {
		GENERAL, HEADER, FOOTER, PLAYERS, PING, LAYOUT, ANIMATIONS, ICONS, HELP;

		Component title() {
			return Lang.tr("tab." + name().toLowerCase());
		}

		/** Pestañas con campos de plantilla: llevan la barra de insertar. */
		boolean text() {
			return this == HEADER || this == FOOTER || this == PLAYERS || this == PING
					|| this == ANIMATIONS;
		}
	}

	/** EFFECT_EDIT va al final para no mover los índices que usa la autoprueba. */
	private enum Popup { NONE, COLOR, EFFECT, ICON, DATA, EFFECT_EDIT }

	private static final ResourceLocation PANEL = ChaosTablist.id("panel");
	private static final ResourceLocation INSET = ChaosTablist.id("inset");
	private static final ResourceLocation CELL = ChaosTablist.id("cell");

	private static final int M = 4;
	private static final int ROW = 19;
	private static final int FH = 16;
	private static final int LIST_W = 64;
	private static final int GOLD = 0xFFE8B23A;
	private static final int GOLD_LIGHT = 0xFFFFE08A;
	private static final int INK = 0xFF0C0704;
	private static final int ROW_SELECTED = 0xFF5A3A14;
	private static final int ROW_HOVER = 0xFF3A2410;
	private static final int LABEL = 0xE8C77A;
	private static final int HINT = 0x9A8466;
	private static final int FIELD_TEXT = 0xFFEBC0;
	private static final int TAG_TEXT = 0xC79BFF;
	private static final int PLACEHOLDER_TEXT = 0x6FE3FF;

	private final boolean chaosRanks;
	private TabConfig original;
	private TabConfig draft;
	private Tab tab = Tab.GENERAL;
	private Popup popup = Popup.NONE;
	@Nullable private Consumer<String> colorTarget;
	private int popupScroll;
	private int rowsScroll;
	private int rowsMax;
	private int listScroll;
	private int selectedAnimation;
	@Nullable private UUID selectedPlayer;
	private boolean autoApply;
	private boolean vanillaPreview;
	private int fakePlayers = 6;
	private boolean dirty;
	private long lastEdit;
	private long previewAnimStart;
	private double previewOpenedAt = ClientContext.now();
	private Component status = Component.empty();
	private long statusUntil;

	@Nullable private GuiEventListener lastText;
	private final List<EditBox> templateBoxes = new ArrayList<>();
	private final List<MultiLineEditBox> templateAreas = new ArrayList<>();
	private final List<int[]> fields = new ArrayList<>();
	private final List<Label> labels = new ArrayList<>();
	private final List<Swatch> swatches = new ArrayList<>();
	@Nullable private EditBox hexBox;
	@Nullable private GoldButton hexOk;
	@Nullable private ListArea list;
	@Nullable private int[] scrollInfo;
	private final List<Preview> previews = new ArrayList<>();

	// Editor de líneas (header, footer, frames de animaciones).
	private int selectedLine;
	private int lineScroll;
	private boolean codeMode;
	@Nullable private int[] lineArea;
	@Nullable private List<String> lineTarget;
	/** Línea que se está editando en su propia fila (doble clic), o -1. */
	private int inlineLine = -1;
	@Nullable private EditBox inlineBox;
	private int lastLineClick = -1;
	private long lastLineClickAt;
	private long linesHoverSince;

	// Deshacer / rehacer (Ctrl+Z / Ctrl+Y): copias de toda la configuración. Lo que se escribe seguido (sin
	// parar más de HISTORY_PAUSE ms) cuenta como un solo paso.
	private static final int HISTORY_MAX = 100;
	private static final long HISTORY_PAUSE = 700;
	private final Deque<String> undoStack = new ArrayDeque<>();
	private final Deque<String> redoStack = new ArrayDeque<>();
	/** La configuración antes de la tanda de cambios en curso. */
	private String stableJson;
	private boolean editBurst;
	private long lastChangeAt;
	private boolean suppressHistory;

	// Selector de color.
	private int pickRgb = 0xFFFFFF;
	private int pickAlpha = 255;
	private boolean draggingAlpha;
	private boolean syncingHex;
	@Nullable private GoldButton hexPreview;
	/** Desplegable al que se vuelve al elegir un color o un icono (el editor de efectos). */
	private Popup returnTo = Popup.NONE;
	/** Valor del campo antes de la vista previa del color (para dejarlo como estaba si no se pulsa OK). */
	@Nullable private String colorOriginal;
	@Nullable private EditBox previewBox;
	@Nullable private String previewBoxValue;
	private int previewBoxCursor;
	private int previewBoxAnchor;
	private boolean colorPreviewing;

	// Editor de un efecto (botón ✎).
	@Nullable private Snippets.Snippet editing;
	private final Map<String, String> editValues = new HashMap<>();
	private String editText = "";
	private boolean editIcon;
	private String editIconTag = "<icon:star>";
	@Nullable private Consumer<String> iconPick;
	private final List<AbstractWidget> popupWidgets = new ArrayList<>();
	private final List<Swatch> popupSwatches = new ArrayList<>();
	private final List<Label> popupLabels = new ArrayList<>();

	/** Vista previa de una plantilla dentro del formulario. */
	private record Preview(int x, int y, int w, Supplier<String> template, Supplier<Map<String, String>> values) {}

	/** Fila de las listas de efectos y datos: cabecera (header != null) o elemento con su ejemplo. */
	private record PopupRow(@Nullable Component header, String label, String insert, String example, Component desc,
			@Nullable Snippets.Snippet snippet) {}

	/** Texto suelto; con {@code maxWidth} &gt; 0 se parte en líneas. */
	private record Label(int x, int y, Supplier<Component> text, int color, int maxWidth) {}

	/** Muestra de color ligada a un campo (clic = abrir la paleta para ese campo). */
	private record Swatch(int x, int y, int size, Supplier<String> color, EditBox field) {}

	/** Lista con selección dibujada a mano (grupos, jugadores, animaciones). */
	private record ListArea(int x, int y, int w, int h, List<Component> items, int selected, IntConsumer click) {}

	/** Fila de formulario: su alto y cómo se monta en (x, y, ancho). */
	private record Row(int height, RowBuilder builder) {}

	private interface RowBuilder {
		void build(int x, int y, int w);
	}

	public TablistEditorScreen(TabConfig config, boolean chaosRanks) {
		super(Component.literal("Chaos Tablist"));
		this.chaosRanks = chaosRanks;
		this.original = config.copy();
		this.draft = config.copy();
		this.stableJson = draft.toJson();
	}

	// ---------------------------------------------------------------------------------------------
	// Geometría

	private int leftW() {
		return Mth.clamp((int) (width * 0.56), 230, 380);
	}

	private int previewX() {
		return M + leftW() + 4;
	}

	private int previewW() {
		return width - M - previewX();
	}

	/** Posiciones de las pestañas: {x, y, ancho} (se reparten en filas según el ancho de la pantalla). */
	private List<int[]> tabLayout() {
		List<int[]> out = new ArrayList<>();
		int x = M;
		int y = 22;
		for (Tab t : Tab.values()) {
			int w = font.width(t.title()) + 12;
			if (x + w > width - M && x > M) {
				x = M;
				y += 18;
			}
			out.add(new int[] {x, y, w});
			x += w + 2;
		}
		return out;
	}

	private int contentTop() {
		List<int[]> tabs = tabLayout();
		return tabs.get(tabs.size() - 1)[1] + 16 + 4;
	}

	private int bottomY() {
		return height - 21;
	}

	private int insertY() {
		return bottomY() - 4 - FH - 4;
	}

	/** Zona útil de la pestaña: {x, y, ancho, alto} (sin la barra de insertar). */
	private int[] content() {
		int x = M + 5;
		int y = contentTop() + 5;
		int bottom = tab.text() ? insertY() - 3 : bottomY() - 8;
		return new int[] {x, y, leftW() - 10, bottom - y};
	}

	// ---------------------------------------------------------------------------------------------
	// Widgets

	private GoldButton button(int x, int y, int w, int h, Component text, Runnable action) {
		return addRenderableWidget(new GoldButton(x, y, w, h, text, b -> action.run()));
	}

	/** Botón tan ancho como su texto. */
	private GoldButton fit(int x, int y, Component text, Runnable action) {
		return button(x, y, font.width(text) + 12, FH + 2, text, action);
	}

	/** Tooltip de vanilla con la explicación de {@code key} (sale al dejar el ratón encima un momento). */
	private <T extends AbstractWidget> T tip(T widget, String key) {
		widget.setTooltip(Tooltip.create(Lang.tr(key)));
		widget.setTooltipDelay(Duration.ofMillis(300));
		return widget;
	}

	/** Campo de una línea sin el borde de vanilla (el recuadro lo pinta el menú). Las plantillas se colorean. */
	private EditBox field(int x, int y, int w, int maxLength, String value, boolean template, Consumer<String> onChange) {
		fields.add(new int[] {x, y, w, FH});
		EditBox box = new EditBox(font, x + 4, y + 4, w - 8, 10, Component.empty());
		box.setBordered(false);
		box.setTextColor(FIELD_TEXT);
		box.setMaxLength(maxLength);
		box.setValue(value == null ? "" : value);
		box.moveCursorToStart(false);
		if (template) {
			box.setFormatter((text, offset) -> highlight(box.getValue(), offset, text));
			templateBoxes.add(box);
		}
		box.setResponder(text -> {
			onChange.accept(text);
			changed();
		});
		addRenderableWidget(box);
		return box;
	}

	private MultiLineEditBox area(int x, int y, int w, int h, String value, Consumer<String> onChange) {
		MultiLineEditBox box = new MultiLineEditBox(font, x, y, w, h, Lang.tr("screen.lines_hint"), Component.empty());
		box.setValue(value);
		((MultiLineEditBoxAccessor) box).chaostablist$textField().seekCursor(Whence.ABSOLUTE, 0);
		box.setValueListener(text -> {
			onChange.accept(text);
			changed();
		});
		templateAreas.add(box);
		addRenderableWidget(box);
		return box;
	}

	private void label(int x, int y, Component text, int maxWidth) {
		labels.add(new Label(x, y, () -> text, LABEL, -maxWidth));
	}

	private void hint(int x, int y, Component text, int maxWidth) {
		labels.add(new Label(x, y, () -> text, HINT, maxWidth));
	}

	/** Ancho de la columna de etiquetas de un formulario de ancho {@code w}. */
	private static int labelW(int w) {
		return Math.min(110, (int) (w * 0.42));
	}

	private Row row(RowBuilder b) {
		return new Row(ROW, b);
	}

	/** Texto de ayuda partido en líneas. */
	private Row hintRow(Component text, int w) {
		int lines = Math.max(1, font.split(text, w).size());
		return new Row(lines * 10 + 3, (x, y, rw) -> hint(x, y + 1, text, rw));
	}

	private Row textRow(Component name, Supplier<String> get, Consumer<String> set, boolean template) {
		return row((x, y, w) -> {
			int lw = labelW(w);
			label(x, y + 4, name, lw - 4);
			field(x + lw, y, w - lw, TabConfig.MAX_TEMPLATE, get.get(), template, set);
		});
	}

	/** Etiqueta y [-] valor [+] a la derecha. */
	private Row stepper(Component name, IntSupplier get, IntConsumer set, int min, int max, int step) {
		return row((x, y, w) -> {
			label(x, y + 4, name, w - 70);
			labels.add(new Label(x + w - 51, y + 4, () -> Component.literal(String.valueOf(get.getAsInt())), 0xFFFFFF, 0));
			button(x + w - 66, y, 14, FH, Component.literal("-"), () -> {
				set.accept(Mth.clamp(get.getAsInt() - step, min, max));
				changed();
			});
			button(x + w - 14, y, 14, FH, Component.literal("+"), () -> {
				set.accept(Mth.clamp(get.getAsInt() + step, min, max));
				changed();
			});
		});
	}

	private Row floatStepper(Component name, Supplier<Float> get, Consumer<Float> set, float min, float max, float step) {
		return row((x, y, w) -> {
			label(x, y + 4, name, w - 70);
			labels.add(new Label(x + w - 51, y + 4, () -> Component.literal(String.format("%.2f", get.get())), 0xFFFFFF, 0));
			button(x + w - 66, y, 14, FH, Component.literal("-"), () -> {
				set.accept(Mth.clamp(Math.round((get.get() - step) * 100) / 100f, min, max));
				changed();
			});
			button(x + w - 14, y, 14, FH, Component.literal("+"), () -> {
				set.accept(Mth.clamp(Math.round((get.get() + step) * 100) / 100f, min, max));
				changed();
			});
		});
	}

	private Row toggle(Component name, BooleanSupplier get, Consumer<Boolean> set) {
		return row((x, y, w) -> {
			int bw = Math.max(40, Math.min(70, w / 3));
			label(x, y + 4, name, w - bw - 4);
			button(x + w - bw, y, bw, FH, Lang.yesNo(get.getAsBoolean()), () -> {
				set.accept(!get.getAsBoolean());
				changed();
				rebuildWidgets();
			});
		});
	}

	private Row cycle(Component name, String[] options, Supplier<String> get, Consumer<String> set) {
		return row((x, y, w) -> {
			int lw = labelW(w);
			label(x, y + 4, name, lw - 4);
			button(x + lw, y, w - lw, FH, Lang.tr("opt." + get.get()), () -> {
				int i = 0;
				for (int k = 0; k < options.length; k++) {
					if (options[k].equals(get.get())) {
						i = k;
					}
				}
				set.accept(options[(i + 1) % options.length]);
				changed();
				rebuildWidgets();
			});
		});
	}

	/** Color #AARRGGBB: campo hexadecimal y muestra (clic en la muestra = paleta). */
	private Row colorRow(Component name, Supplier<String> get, Consumer<String> set) {
		return row((x, y, w) -> {
			int lw = labelW(w);
			label(x, y + 4, name, lw - 4);
			EditBox box = field(x + lw, y, w - lw - 18, 9, get.get(), false, text -> {
				if (ColorUtil.parse(text) != null) {
					set.accept(text.toUpperCase());
				}
			});
			swatches.add(new Swatch(x + w - 15, y + 1, 14, get, box));
		});
	}

	/** Filas con scroll (la rueda del ratón mueve {@link #rowsScroll}). */
	private void rows(List<Row> rows, int x, int y, int w, int h) {
		int total = rows.stream().mapToInt(Row::height).sum();
		if (total > h) {
			h -= 10; // sitio para el indicador de scroll
		}
		// Primera fila desde la que el resto ya cabe entero: el scroll máximo.
		int used = 0;
		int max = rows.size();
		for (int i = rows.size() - 1; i >= 0; i--) {
			used += rows.get(i).height();
			if (used > h) {
				break;
			}
			max = i;
		}
		rowsMax = max;
		rowsScroll = Mth.clamp(rowsScroll, 0, rowsMax);
		int cy = y;
		for (int i = rowsScroll; i < rows.size(); i++) {
			Row r = rows.get(i);
			if (cy + r.height() > y + h + 2) {
				break;
			}
			r.builder().build(x, cy, w);
			cy += r.height();
		}
		scrollInfo = rowsMax > 0 ? new int[] {x + w, y + h, rowsScroll, rowsMax} : null;
	}

	@Override
	protected void init() {
		templateBoxes.clear();
		templateAreas.clear();
		fields.clear();
		labels.clear();
		swatches.clear();
		list = null;
		lastText = null;
		rowsMax = 0;
		scrollInfo = null;
		previews.clear();
		lineArea = null;
		lineTarget = null;
		inlineBox = null;

		// Pestañas.
		List<int[]> layout = tabLayout();
		Tab[] tabs = Tab.values();
		for (int i = 0; i < tabs.length; i++) {
			Tab t = tabs[i];
			int[] p = layout.get(i);
			tip(button(p[0], p[1], p[2], 16, t.title(), () -> {
				closePopup();
				tab = t;
				rowsScroll = 0;
				listScroll = 0;
				popupScroll = 0;
				selectedLine = 0;
				lineScroll = 0;
				inlineLine = -1;
				rebuildWidgets();
			}), "tab." + t.name().toLowerCase() + ".tip").selected(tab == t);
		}

		int[] c = content();
		switch (tab) {
			case GENERAL -> initGeneral(c);
			case HEADER -> initLines(c, true);
			case FOOTER -> initLines(c, false);
			case PLAYERS -> initPlayers(c);
			case PING -> initPing(c);
			case LAYOUT -> initLayout(c);
			case ANIMATIONS -> initAnimations(c);
			case ICONS, HELP -> {}
		}

		// Barra de insertar.
		if (tab.text()) {
			int bw = (c[2] - 6) / 4;
			int by = insertY();
			tip(button(c[0], by, bw, FH, Lang.tr("insert.color"), () -> openPopup(Popup.COLOR, null)), "insert.color.tip");
			tip(button(c[0] + bw + 2, by, bw, FH, Lang.tr("insert.effect"), () -> openPopup(Popup.EFFECT, null)),
					"insert.effect.tip");
			tip(button(c[0] + 2 * (bw + 2), by, bw, FH, Lang.tr("insert.icon"), () -> openPopup(Popup.ICON, null)),
					"insert.icon.tip");
			tip(button(c[0] + 3 * (bw + 2), by, bw, FH, Lang.tr("insert.data"), () -> openPopup(Popup.DATA, null)),
					"insert.data.tip");
		}

		// Campo hexadecimal del selector de color (solo visible con ese desplegable abierto).
		int[] pr = popupRect();
		int hexY = pr[1] + pr[3] - 21;
		hexBox = new EditBox(font, pr[0] + 7 + 30 + 4, hexY + 4, 56, 10, Component.empty());
		hexBox.setBordered(false);
		hexBox.setTextColor(FIELD_TEXT);
		hexBox.setMaxLength(9);
		syncHex();
		hexBox.setResponder(text -> {
			Integer col = ColorUtil.parse(text);
			if (col != null && !syncingHex) {
				pickRgb = col & 0xFFFFFF;
				pickAlpha = (col >>> 24) & 0xFF;
				refreshColorPreview();
			}
		});
		hexOk = tip(new GoldButton(pr[0] + pr[2] - 7 - 36, hexY, 36, FH, Lang.tr("button.ok"),
				b -> pickColor(pickAlpha << 24 | pickRgb)), "button.ok.tip");
		Component pv = Lang.tr("button.preview");
		int pvw = font.width(pv) + 10;
		hexPreview = tip(new GoldButton(hexOk.getX() - 2 - pvw, hexY, pvw, FH, pv, b -> toggleColorPreview()),
				"button.preview.tip");
		updatePopupWidgets();
		if (popup == Popup.EFFECT_EDIT) {
			buildEffectEditor();
		}

		// Barra de abajo.
		int by = bottomY();
		int bx = M;
		bx += tip(fit(bx, by, Lang.tr("button.save"), this::save), "button.save.tip").getWidth() + 2;
		bx += tip(fit(bx, by, Lang.tr("button.discard"), () -> {
			checkpoint();
			draft = original.copy();
			settleHistory();
			dirty = false;
			TabRenderer.invalidate();
			rebuildWidgets();
		}), "button.discard.tip").getWidth() + 2;
		bx += tip(fit(bx, by, Lang.tr("button.defaults"), () -> {
			checkpoint();
			draft = new TabConfig();
			draft.sanitize();
			suppressHistory = true;
			changed();
			suppressHistory = false;
			settleHistory();
			rebuildWidgets();
		}), "button.defaults.tip").getWidth() + 2;
		tip(fit(bx, by, Lang.tr("button.auto", Lang.yesNo(autoApply)), () -> {
			autoApply = !autoApply;
			rebuildWidgets();
		}), "button.auto.tip");
		Component close = Lang.tr("button.close");
		tip(fit(width - M - font.width(close) - 12, by, close, this::onClose), "button.close.tip");

		// Controles de la vista previa (dentro de su panel, abajo).
		int px = previewX() + 4;
		int py = bottomY() - 4 - FH - 3;
		px += tip(button(px, py, font.width(Lang.tr("button.view_vanilla")) + 10, FH,
				Lang.tr(vanillaPreview ? "button.view_vanilla" : "button.view_mod"), () -> {
					vanillaPreview = !vanillaPreview;
					rebuildWidgets();
				}), "button.view.tip").getWidth() + 2;
		px += tip(button(px, py, 16, FH, Component.literal("▶"), () -> {
			previewAnimStart = Util.getMillis();
			previewOpenedAt = ClientContext.now();
		}), "button.replay.tip").getWidth() + 2;
		px += tip(button(px, py, 14, FH, Component.literal("-"), () -> fakePlayers = Math.max(0, fakePlayers - 1)),
				"button.fakes.tip").getWidth() + 2;
		labels.add(new Label(px + 1, py + 4, () -> Component.empty()
				.append(Component.literal(String.valueOf(Icons.icon("players").glyph())).withStyle(Style.EMPTY.withFont(Icons.FONT_COMPACT)))
				.append(" " + fakePlayers), LABEL, 0));
		px += font.width(" 20") + 12;
		tip(button(px, py, 14, FH, Component.literal("+"), () -> fakePlayers = Math.min(TabRenderer.MAX_FAKES, fakePlayers + 1)),
				"button.fakes.tip");
	}

	// ----- Pestañas

	private void initGeneral(int[] c) {
		int w = c[2];
		List<Row> rows = new ArrayList<>();
		rows.add(toggle(Lang.tr("field.enabled"), () -> draft.enabled, v -> draft.enabled = v));
		rows.add(textRow(Lang.tr("field.server_name"), () -> draft.serverName, v -> draft.serverName = v, false));
		rows.add(stepper(Lang.tr("field.update_ticks"), () -> draft.updateTicks, v -> draft.updateTicks = v, 1, 200, 1));
		rows.add(textRow(Lang.tr("field.client_fallback"), () -> draft.clientOnlyFallback, v -> draft.clientOnlyFallback = v, false));
		rows.add(textRow(Lang.tr("field.sort_order"), () -> draft.sorting.order, v -> draft.sorting.order = v, false));
		String[] keys = {"rank", "name", "ping", "gamemode", "world", "team"};
		rows.add(row((x, y, rw) -> sortButtons(x, y, rw, keys, 0, 3)));
		rows.add(row((x, y, rw) -> sortButtons(x, y, rw, keys, 3, 6)));
		rows.add(hintRow(Lang.tr("screen.hint.general"), w));
		rows.add(toggle(Lang.tr("field.spectators_last"), () -> draft.layout.spectatorsLast, v -> draft.layout.spectatorsLast = v));
		rows.add(hintRow(chaosRanks ? Lang.tr("screen.chaosranks_on") : Lang.tr("screen.chaosranks_off"), w));
		rows(rows, c[0], c[1], c[2], c[3]);
	}

	/** Botones de las claves de orden (pulsar = añadir o quitar). */
	private void sortButtons(int x, int y, int w, String[] keys, int from, int to) {
		int n = to - from;
		int bw = (w - 2 * (n - 1)) / n;
		List<String> order = new ArrayList<>(List.of(draft.sorting.order.split(",")));
		order.replaceAll(String::trim);
		for (int i = from; i < to; i++) {
			String k = keys[i];
			int pos = order.indexOf(k);
			Component text = pos >= 0 ? Component.literal((pos + 1) + ". ").append(Lang.tr("sort." + k)) : Lang.tr("sort." + k);
			button(x + (i - from) * (bw + 2), y, bw, FH, text, () -> {
				List<String> o = new ArrayList<>(List.of(draft.sorting.order.split(",")));
				o.replaceAll(String::trim);
				if (!o.remove(k)) {
					o.add(k);
				}
				o.removeIf(String::isEmpty);
				draft.sorting.order = String.join(",", o);
				changed();
				rebuildWidgets();
			}).selected(false);
		}
	}

	private void initLines(int[] c, boolean header) {
		List<String> lines = header ? draft.header : draft.footer;
		lineEditor(c[0], c[1], c[2], c[3], Lang.tr(header ? "screen.header" : "screen.footer"), lines);
	}

	/**
	 * Lista de líneas que se ven ya pintadas (con sus efectos); la elegida se edita en el campo de abajo y su fila
	 * cambia en vivo mientras se escribe. "Código" cambia al cuadro de texto de siempre para pegar muchas líneas.
	 */
	private void lineEditor(int x, int y, int w, int h, Component title, List<String> lines) {
		Component toggle = Lang.tr(codeMode ? "button.visual" : "button.code");
		int tw = font.width(toggle) + 12;
		label(x, y + 4, title, w - tw - 4);
		tip(button(x + w - tw, y, tw, FH, toggle, () -> {
			codeMode = !codeMode;
			inlineLine = -1;
			rebuildWidgets();
		}), "button.code.tip");
		int top = y + FH + 3;
		if (codeMode) {
			MultiLineEditBox box = area(x, top, w, y + h - top, String.join("\n", lines), text -> {
				List<String> parts = new ArrayList<>(List.of(text.split("\n", -1)));
				lines.clear();
				lines.addAll(parts.subList(0, Math.min(parts.size(), TabConfig.MAX_LINES)));
			});
			setInitialFocus(box);
			return;
		}
		if (lines.isEmpty()) {
			lines.add("");
		}
		selectedLine = Mth.clamp(selectedLine, 0, lines.size() - 1);
		int editY = y + h - FH;
		lineArea = new int[] {x, top, w, editY - 3 - top};
		lineTarget = lines;
		int sel = selectedLine;

		int bx = x + w - 4 * 16 + 2;
		EditBox edit = tip(field(x, editY, bx - x - 2, TabConfig.MAX_TEMPLATE, lines.get(sel), true, v -> {
			if (sel < lines.size()) {
				lines.set(sel, v);
			}
		}), "screen.line_field.tip");
		setInitialFocus(edit);

		// Doble clic en una línea: se edita ahí mismo, en su fila.
		if (inlineLine >= 0 && inlineLine < lines.size()) {
			int visible = Math.max(1, (lineArea[3] - 4) / 12);
			lineScroll = Mth.clamp(lineScroll, Math.max(0, inlineLine - visible + 1), inlineLine);
			int iy = top + 2 + (inlineLine - lineScroll) * 12;
			int line = inlineLine;
			EditBox box = new EditBox(font, x + 20, iy + 2, w - 24, 10, Component.empty());
			box.setBordered(false);
			box.setTextColor(FIELD_TEXT);
			box.setMaxLength(TabConfig.MAX_TEMPLATE);
			box.setValue(lines.get(line));
			box.setFormatter((text, offset) -> highlight(box.getValue(), offset, text));
			box.setResponder(v -> {
				if (line < lines.size()) {
					lines.set(line, v);
				}
				changed();
			});
			templateBoxes.add(box);
			addRenderableWidget(box);
			inlineBox = box;
			setInitialFocus(box);
		} else {
			inlineLine = -1;
		}
		tip(button(bx, editY, 14, FH, Component.literal("▲"), () -> moveLine(lines, -1)), "button.line_up.tip").active = sel > 0;
		tip(button(bx + 16, editY, 14, FH, Component.literal("▼"), () -> moveLine(lines, 1)), "button.line_down.tip").active =
				sel < lines.size() - 1;
		tip(button(bx + 32, editY, 14, FH, Component.literal("+"), () -> {
			lines.add(selectedLine + 1, "");
			selectedLine++;
			inlineLine = -1;
			changed();
			rebuildWidgets();
		}), "button.line_add.tip").active = lines.size() < TabConfig.MAX_LINES;
		tip(button(bx + 48, editY, 14, FH, Component.literal("✖"), () -> {
			inlineLine = -1;
			if (lines.size() > 1) {
				lines.remove(selectedLine);
				selectedLine = Math.max(0, selectedLine - 1);
			} else {
				lines.set(0, "");
			}
			changed();
			rebuildWidgets();
		}), "button.line_remove.tip");
	}

	private void moveLine(List<String> lines, int dir) {
		int to = selectedLine + dir;
		if (to < 0 || to >= lines.size()) {
			return;
		}
		String s = lines.remove(selectedLine);
		lines.add(to, s);
		selectedLine = to;
		inlineLine = -1;
		changed();
		rebuildWidgets();
	}

	/** Fila "Así se ve:" con la plantilla ya pintada. */
	private Row previewRow(Supplier<String> template, Supplier<Map<String, String>> values) {
		return new Row(14, (x, y, w) -> {
			Component name = Lang.tr("screen.looks_like");
			hint(x, y + 3, name, 0);
			int lx = font.width(name) + 4;
			previews.add(new Preview(x + lx, y + 3, w - lx, template, values));
		});
	}

	/** Lista a la izquierda con [+] [-] debajo; devuelve el área que queda a la derecha. */
	private int[] sideList(int[] c, List<Component> items, int selected, IntConsumer click, @Nullable Runnable add,
			@Nullable Runnable remove) {
		boolean buttons = add != null || remove != null;
		list = new ListArea(c[0], c[1], LIST_W, c[3] - (buttons ? FH + 2 : 0), items, selected, click);
		if (add != null) {
			button(c[0], c[1] + c[3] - FH, LIST_W / 2 - 1, FH, Component.literal("+"), add);
		}
		if (remove != null) {
			button(c[0] + LIST_W / 2 + 1, c[1] + c[3] - FH, LIST_W / 2 - 1, FH, Component.literal("-"), remove);
		}
		return new int[] {c[0] + LIST_W + 5, c[1], c[2] - LIST_W - 5, c[3]};
	}

	private void initPlayers(int[] c) {
		// Jugadores conectados + los que ya tienen cambios guardados.
		LinkedHashMap<UUID, String> people = new LinkedHashMap<>();
		if (minecraft != null && minecraft.getConnection() != null) {
			for (PlayerInfo info : minecraft.getConnection().getListedOnlinePlayers()) {
				people.put(info.getProfile().getId(), info.getProfile().getName());
			}
		}
		draft.players.forEach((id, o) -> {
			try {
				people.putIfAbsent(UUID.fromString(id), o.name.isEmpty() ? id.substring(0, 8) : o.name);
			} catch (IllegalArgumentException ignored) {
				// UUID inválido escrito a mano.
			}
		});
		List<UUID> ids = new ArrayList<>(people.keySet());
		if (selectedPlayer == null || !people.containsKey(selectedPlayer)) {
			selectedPlayer = ids.isEmpty() ? null : ids.get(0);
		}
		List<Component> items = new ArrayList<>();
		for (UUID id : ids) {
			boolean custom = draft.players.containsKey(id.toString());
			items.add(Component.literal((custom ? "✎ " : "") + people.get(id)).withColor(custom ? 0xFFC300 : 0xFFFFFF));
		}
		int[] f = sideList(c, items, ids.indexOf(selectedPlayer), i -> {
			selectedPlayer = ids.get(i);
			rebuildWidgets();
		}, null, null);
		if (selectedPlayer == null) {
			hint(f[0], f[1] + 4, Lang.tr("screen.no_players"), f[2]);
			return;
		}
		String key = selectedPlayer.toString();
		String name = people.get(selectedPlayer);
		Supplier<TabConfig.PlayerOverride> o = () -> draft.players.computeIfAbsent(key, k -> {
			TabConfig.PlayerOverride p = new TabConfig.PlayerOverride();
			p.name = name;
			return p;
		});
		TabConfig.PlayerOverride current = draft.players.get(key);
		List<Row> rows = new ArrayList<>();
		rows.add(row((x, y, w) -> label(x, y + 4, Component.literal(name), w)));
		rows.add(textRow(Lang.tr("field.prefix"), () -> current == null || current.prefix == null ? "" : current.prefix,
				v -> o.get().prefix = v.isEmpty() ? null : v, true));
		rows.add(textRow(Lang.tr("field.name"), () -> current == null || current.format == null ? "" : current.format,
				v -> o.get().format = v.isEmpty() ? null : v, true));
		rows.add(textRow(Lang.tr("field.suffix"), () -> current == null || current.suffix == null ? "" : current.suffix,
				v -> o.get().suffix = v.isEmpty() ? null : v, true));
		UUID pid = selectedPlayer;
		rows.add(previewRow(() -> draft.rowTemplate(key), () -> {
			Map<String, String> v = new HashMap<>(ClientState.player(pid));
			v.putIfAbsent("player", name);
			return v;
		}));
		rows.add(toggle(Lang.tr("field.hidden"), () -> current != null && current.hidden, v -> o.get().hidden = v));
		rows.add(row((x, y, w) -> fit(x, y, Lang.tr("button.reset_player"), () -> {
			draft.players.remove(key);
			changed();
			rebuildWidgets();
		})));
		rows.add(hintRow(Lang.tr("screen.hint.players"), f[2]));
		rows(rows, f[0], f[1], f[2], f[3]);
	}

	private void initPing(int[] c) {
		TabConfig.Ping p = draft.ping;
		List<Row> rows = new ArrayList<>();
		rows.add(cycle(Lang.tr("field.ping_style"), new String[] {"both", "bars", "number", "text", "hidden"},
				() -> p.style, v -> p.style = v));
		rows.add(textRow(Lang.tr("field.ping_format"), () -> p.format, v -> p.format = v, true));
		rows.add(previewRow(() -> p.format, this::selfValues));
		rows.add(textRow(Lang.tr("field.ping_vanilla"), () -> p.vanillaSuffix, v -> p.vanillaSuffix = v, true));
		rows.add(previewRow(() -> "{player}" + p.vanillaSuffix, this::selfValues));
		rows.add(new Row(13, (x, y, w) -> label(x, y + 3, Lang.tr("screen.thresholds"), w)));
		for (int i = 0; i < p.thresholds.size(); i++) {
			TabConfig.Threshold t = p.thresholds.get(i);
			int idx = i;
			boolean last = t.max == Integer.MAX_VALUE;
			rows.add(row((x, y, w) -> {
				label(x, y + 4, last ? Lang.tr("screen.threshold_rest") : Lang.tr("screen.threshold_max"), 40);
				if (!last) {
					field(x + 40, y, 44, 6, String.valueOf(t.max), false, v -> {
						try {
							t.max = Mth.clamp(Integer.parseInt(v.trim()), 0, 99999);
						} catch (NumberFormatException ignored) {
							// Se ignora hasta que sea un número.
						}
					});
				}
				EditBox box = field(x + 88, y, w - 88 - 36, 9, t.color, false, v -> {
					if (ColorUtil.parse(v) != null) {
						t.color = v;
					}
				});
				swatches.add(new Swatch(x + w - 33, y + 1, 14, () -> t.color, box));
				if (!last) {
					button(x + w - 14, y, 14, FH, Component.literal("-"), () -> {
						p.thresholds.remove(idx);
						changed();
						rebuildWidgets();
					});
				}
			}));
		}
		rows.add(row((x, y, w) -> fit(x, y, Lang.tr("button.add_threshold"), () -> {
			int max = p.thresholds.size() > 1 ? p.thresholds.get(p.thresholds.size() - 2).max + 100 : 100;
			p.thresholds.add(new TabConfig.Threshold(max, "#FFFFFF"));
			p.thresholds.sort((a, b) -> Integer.compare(a.max, b.max));
			changed();
			rebuildWidgets();
		})));
		rows(rows, c[0], c[1], c[2], c[3]);
	}

	private void initLayout(int[] c) {
		TabConfig.Layout l = draft.layout;
		List<Row> rows = new ArrayList<>();
		rows.add(colorRow(Lang.tr("field.panel_color"), () -> l.panelColor, v -> l.panelColor = v));
		rows.add(colorRow(Lang.tr("field.panel_color2"), () -> l.panelColor2, v -> l.panelColor2 = v));
		rows.add(colorRow(Lang.tr("field.border_color"), () -> l.borderColor, v -> l.borderColor = v));
		rows.add(colorRow(Lang.tr("field.border_color2"), () -> l.borderColor2, v -> l.borderColor2 = v));
		rows.add(stepper(Lang.tr("field.border_width"), () -> l.borderWidth, v -> l.borderWidth = v, 0, 4, 1));
		rows.add(toggle(Lang.tr("field.animated_border"), () -> l.animatedBorder, v -> l.animatedBorder = v));
		rows.add(toggle(Lang.tr("field.rounded"), () -> l.rounded, v -> l.rounded = v));
		rows.add(colorRow(Lang.tr("field.row_color"), () -> l.rowColor, v -> l.rowColor = v));
		rows.add(colorRow(Lang.tr("field.row_alt_color"), () -> l.rowAltColor, v -> l.rowAltColor = v));
		rows.add(colorRow(Lang.tr("field.self_row_color"), () -> l.selfRowColor, v -> l.selfRowColor = v));
		rows.add(colorRow(Lang.tr("field.separator_color"), () -> l.separatorColor, v -> l.separatorColor = v));
		rows.add(cycle(Lang.tr("field.header_align"), new String[] {"center", "left", "right"},
				() -> l.headerAlign, v -> l.headerAlign = v));
		rows.add(cycle(Lang.tr("field.open_animation"), new String[] {"drop", "fade", "slide", "scale", "none"},
				() -> l.openAnimation, v -> {
					l.openAnimation = v;
					previewAnimStart = Util.getMillis();
					previewOpenedAt = ClientContext.now();
				}));
		rows.add(stepper(Lang.tr("field.open_millis"), () -> l.openMillis, v -> l.openMillis = v, 0, 2000, 20));
		rows.add(floatStepper(Lang.tr("field.scale"), () -> l.scale, v -> l.scale = v, 0.5f, 2f, 0.05f));
		rows.add(stepper(Lang.tr("field.row_height"), () -> l.rowHeight, v -> l.rowHeight = v, 8, 24, 1));
		rows.add(stepper(Lang.tr("field.row_gap"), () -> l.rowGap, v -> l.rowGap = v, 0, 8, 1));
		rows.add(stepper(Lang.tr("field.column_gap"), () -> l.columnGap, v -> l.columnGap = v, 0, 40, 1));
		rows.add(stepper(Lang.tr("field.padding"), () -> l.padding, v -> l.padding = v, 0, 30, 1));
		rows.add(stepper(Lang.tr("field.max_rows"), () -> l.maxRows, v -> l.maxRows = v, 1, 60, 1));
		rows.add(stepper(Lang.tr("field.max_players"), () -> l.maxPlayers, v -> l.maxPlayers = v, 1, 500, 10));
		rows.add(stepper(Lang.tr("field.min_column"), () -> l.minColumnWidth, v -> l.minColumnWidth = v, 20, 400, 10));
		rows.add(stepper(Lang.tr("field.top_margin"), () -> l.topMargin, v -> l.topMargin = v, 0, 200, 2));
		rows.add(toggle(Lang.tr("field.show_heads"), () -> l.showHeads, v -> l.showHeads = v));
		rows.add(toggle(Lang.tr("field.text_shadow"), () -> l.textShadow, v -> l.textShadow = v));
		rows.add(toggle(Lang.tr("field.singleplayer"), () -> l.showInSingleplayer, v -> l.showInSingleplayer = v));
		rows.add(floatStepper(Lang.tr("field.spectator_alpha"), () -> l.spectatorAlpha, v -> l.spectatorAlpha = v, 0.1f, 1f, 0.05f));
		rows.add(cycle(Lang.tr("field.objective_style"), new String[] {"number", "hearts", "hidden"},
				() -> l.objectiveStyle, v -> l.objectiveStyle = v));
		rows(rows, c[0], c[1], c[2], c[3]);
	}

	private void initAnimations(int[] c) {
		List<String> names = new ArrayList<>(draft.animations.keySet());
		selectedAnimation = Mth.clamp(selectedAnimation, 0, Math.max(0, names.size() - 1));
		List<Component> items = names.stream().map(n -> (Component) Component.literal(n)).toList();
		int[] f = sideList(c, items, selectedAnimation, i -> {
			selectedAnimation = i;
			selectedLine = 0;
			lineScroll = 0;
			inlineLine = -1;
			rebuildWidgets();
		}, () -> {
			String id = freeId("anim", names);
			draft.animations.put(id, TabConfig.Animation.of(1000, "<yellow>Frame 1", "<gold>Frame 2"));
			selectedAnimation = new ArrayList<>(draft.animations.keySet()).indexOf(id);
			changed();
			rebuildWidgets();
		}, () -> {
			if (!names.isEmpty()) {
				draft.animations.remove(names.get(selectedAnimation));
				selectedAnimation = Math.max(0, selectedAnimation - 1);
				changed();
				rebuildWidgets();
			}
		});
		if (names.isEmpty()) {
			hint(f[0], f[1] + 4, Lang.tr("screen.no_animations"), f[2]);
			return;
		}
		TabConfig.Animation a = draft.animations.get(names.get(selectedAnimation));
		int x = f[0];
		int y = f[1];
		int w = f[2];
		int lw = labelW(w);
		label(x, y + 4, Lang.tr("field.id"), lw - 4);
		field(x + lw, y, w - lw, 32, names.get(selectedAnimation), false, v -> {
			String clean = v.toLowerCase().replaceAll("[^a-z0-9_\\-]", "_");
			String now = currentAnimationName();
			if (!clean.isEmpty() && !clean.equals(now) && !draft.animations.containsKey(clean)) {
				// Renombrar sin perder el orden.
				LinkedHashMap<String, TabConfig.Animation> copy = new LinkedHashMap<>();
				draft.animations.forEach((k, an) -> copy.put(k.equals(now) ? clean : k, an));
				draft.animations = copy;
			}
		});
		stepper(Lang.tr("field.interval"), () -> a.intervalMs, v -> a.intervalMs = v, 50, 60000, 50).builder().build(x, y + ROW, w);
		int top = y + 2 * ROW;
		lineEditor(x, top, w, f[1] + f[3] - top, Lang.tr("screen.anim_usage", currentAnimationName()), a.frames);
	}

	private String currentAnimationName() {
		List<String> names = new ArrayList<>(draft.animations.keySet());
		return names.isEmpty() ? "" : names.get(Mth.clamp(selectedAnimation, 0, names.size() - 1));
	}

	private static String freeId(String base, List<String> taken) {
		String id = base;
		for (int i = 2; taken.contains(id); i++) {
			id = base + "_" + i;
		}
		return id;
	}

	// ---------------------------------------------------------------------------------------------
	// Insertar

	private void openPopup(Popup p, @Nullable Consumer<String> target) {
		openPopup(p, target, null);
	}

	private void openPopup(Popup p, @Nullable Consumer<String> target, @Nullable String current) {
		revertColorPreview();
		popup = popup == p && target == null ? Popup.NONE : p;
		colorTarget = target;
		colorOriginal = current;
		returnTo = Popup.NONE;
		iconPick = null;
		popupScroll = 0;
		Integer c = ColorUtil.parse(current);
		if (c != null) {
			pickRgb = c & 0xFFFFFF;
			pickAlpha = (c >>> 24) & 0xFF;
		}
		syncHex();
		updatePopupWidgets();
	}

	/**
	 * Cierra el desplegable sin aceptar: deshace la vista previa del color y vuelve al editor de efectos si se
	 * abrió desde ahí. Con {@code all} se cierra todo (al cambiar de pestaña).
	 */
	private void closePopup(boolean all) {
		revertColorPreview();
		Popup back = all ? Popup.NONE : returnTo;
		returnTo = Popup.NONE;
		colorTarget = null;
		iconPick = null;
		popup = back;
		popupScroll = 0;
		if (popup == Popup.EFFECT_EDIT) {
			buildEffectEditor();
		} else {
			editing = null;
			clearPopupWidgets();
		}
		updatePopupWidgets();
	}

	private void closePopup() {
		closePopup(true);
	}

	private void updatePopupWidgets() {
		if (hexBox != null && hexOk != null && hexPreview != null) {
			boolean show = popup == Popup.COLOR;
			hexBox.visible = show;
			hexOk.visible = show;
			hexPreview.visible = show;
			hexPreview.setMessage(Lang.tr(colorPreviewing ? "button.preview_off" : "button.preview"));
			hexPreview.active = colorTarget != null || lastText instanceof EditBox box && children().contains(box);
		}
	}

	// ----- Vista previa del color (se aplica sin cerrar; si no se pulsa OK se deshace)

	private String pickedHex() {
		int argb = pickAlpha << 24 | pickRgb;
		return (argb >>> 24) == 0xFF ? String.format("#%06X", argb & 0xFFFFFF) : String.format("#%08X", argb);
	}

	private void toggleColorPreview() {
		if (colorPreviewing) {
			revertColorPreview();
		} else {
			colorPreviewing = true;
			if (colorTarget == null && lastText instanceof EditBox box && children().contains(box)) {
				// Recordar el campo tal cual (texto, cursor y selección) para poder dejarlo igual.
				previewBox = box;
				previewBoxValue = box.getValue();
				previewBoxCursor = box.getCursorPosition();
				String sel = box.getHighlighted();
				int start = previewBoxCursor;
				if (!sel.isEmpty() && !box.getValue().startsWith(sel, start)) {
					start = previewBoxCursor - sel.length();
				}
				previewBoxAnchor = sel.isEmpty() ? previewBoxCursor : (start == previewBoxCursor ? start + sel.length() : start);
			}
			refreshColorPreview();
		}
		updatePopupWidgets();
	}

	/** Vuelve a aplicar el color elegido mientras la vista previa está activa (paleta, transparencia, hex). */
	private void refreshColorPreview() {
		if (!colorPreviewing || popup != Popup.COLOR) {
			return;
		}
		String hex = pickedHex();
		suppressHistory = true;
		if (colorTarget != null) {
			colorTarget.accept(hex);
		} else if (previewBox != null && previewBoxValue != null) {
			restorePreviewBox();
			previewBox.insertText(colorTag(hex, previewBox.getHighlighted()));
		}
		suppressHistory = false;
		TabRenderer.invalidate();
	}

	private void restorePreviewBox() {
		if (previewBox == null || previewBoxValue == null) {
			return;
		}
		previewBox.setValue(previewBoxValue);
		previewBox.setCursorPosition(previewBoxCursor);
		previewBox.setHighlightPos(previewBoxAnchor);
	}

	/** Deja todo como antes de la vista previa. */
	private void revertColorPreview() {
		if (!colorPreviewing) {
			return;
		}
		colorPreviewing = false;
		suppressHistory = true;
		if (colorTarget != null && colorOriginal != null) {
			colorTarget.accept(colorOriginal);
		} else {
			restorePreviewBox();
		}
		suppressHistory = false;
		previewBox = null;
		previewBoxValue = null;
		TabRenderer.invalidate();
	}

	/** Etiqueta de color; si hay texto seleccionado, lo colorea solo a él. */
	private static String colorTag(String hex, @Nullable String selected) {
		return selected == null || selected.isEmpty() ? "<" + hex + ">" : "<" + hex + ">" + selected + "</>";
	}

	/** Zona del desplegable: encima del panel izquierdo (deja libre la barra de insertar). */
	private int[] popupRect() {
		int x = M + 2;
		int y = contentTop() + 2;
		return new int[] {x, y, leftW() - 4, insertY() - 2 - y};
	}

	/** OK del selector de color: se queda el color (con o sin vista previa). */
	private void pickColor(int argb) {
		String hex = (argb >>> 24) == 0xFF ? String.format("#%06X", argb & 0xFFFFFF) : String.format("#%08X", argb);
		boolean previewed = colorPreviewing;
		colorPreviewing = false;
		if (colorTarget != null) {
			Consumer<String> target = colorTarget;
			target.accept(hex);
			closePopup(false);
			return;
		}
		if (previewed && previewBox != null) {
			// Ya está metido por la vista previa: se aplica de nuevo sobre el texto original y se deja.
			restorePreviewBox();
			previewBox = null;
			previewBoxValue = null;
		}
		insert(hex, true);
	}

	private void insert(String text) {
		insert(text, false, false);
	}

	private void insert(String text, boolean color) {
		insert(text, color, false);
	}

	/**
	 * Mete el texto en el cursor del último campo de plantilla usado; si no hay, lo copia al portapapeles. Con
	 * {@code color} es una etiqueta de color que envuelve la selección; con {@code exact} va tal cual (el editor
	 * de efectos ya ha puesto la selección como contenido).
	 */
	private void insert(String text, boolean color, boolean exact) {
		closePopup();
		if (lastText instanceof EditBox box && children().contains(box)) {
			box.insertText(color ? colorTag(text, box.getHighlighted()) : exact ? text : wrap(text, box.getHighlighted()));
			setFocused(box);
		} else if (lastText instanceof MultiLineEditBox area && children().contains(area)) {
			var field = ((MultiLineEditBoxAccessor) area).chaostablist$textField();
			field.insertText(color ? colorTag(text, field.getSelectedText()) : exact ? text : wrap(text, field.getSelectedText()));
			setFocused(area);
		} else {
			String copy = color ? colorTag(text, null) : text;
			if (minecraft != null) {
				minecraft.keyboardHandler.setClipboard(copy);
			}
			status(Lang.tr("msg.copied", copy));
			return;
		}
		click();
	}

	/** Si hay texto seleccionado, el efecto lo envuelve en lugar de meter la palabra de ejemplo. */
	private static String wrap(String snippet, String selected) {
		if (selected == null || selected.isEmpty() || !snippet.contains("text")) {
			return snippet;
		}
		return snippet.replace("scrolling text", "text")
				.replaceFirst("text", java.util.regex.Matcher.quoteReplacement(selected));
	}

	@Override
	public void setFocused(@Nullable GuiEventListener listener) {
		super.setFocused(listener);
		if ((listener instanceof EditBox box && templateBoxes.contains(box))
				|| (listener instanceof MultiLineEditBox area && templateAreas.contains(area))) {
			lastText = listener;
		}
	}

	// ---------------------------------------------------------------------------------------------
	// Guardar

	private void changed() {
		dirty = true;
		lastEdit = Util.getMillis();
		TabRenderer.invalidate();
		if (!suppressHistory) {
			if (!editBurst) {
				pushHistory(undoStack, stableJson);
				redoStack.clear();
				editBurst = true;
			}
			lastChangeAt = lastEdit;
		}
	}

	private static void pushHistory(Deque<String> stack, String json) {
		if (!json.equals(stack.peek())) {
			stack.push(json);
			while (stack.size() > HISTORY_MAX) {
				stack.removeLast();
			}
		}
	}

	/** Antes de un cambio de golpe (Descartar, Por defecto): se puede deshacer en un paso. */
	private void checkpoint() {
		pushHistory(undoStack, editBurst ? stableJson : draft.toJson());
		if (editBurst) {
			pushHistory(undoStack, draft.toJson());
		}
		redoStack.clear();
	}

	private void settleHistory() {
		stableJson = draft.toJson();
		editBurst = false;
	}

	private void undo(boolean redo) {
		Deque<String> from = redo ? redoStack : undoStack;
		Deque<String> to = redo ? undoStack : redoStack;
		String current = draft.toJson();
		while (!from.isEmpty()) {
			String json = from.pop();
			if (!json.equals(current)) {
				pushHistory(to, current);
				draft = TabConfig.fromJson(json);
				settleHistory();
				dirty = true;
				lastEdit = Util.getMillis();
				TabRenderer.invalidate();
				status(Lang.tr(redo ? "msg.redo" : "msg.undo"));
				rebuildWidgets();
				return;
			}
		}
		status(Lang.tr(redo ? "msg.nothing_redo" : "msg.nothing_undo"));
	}

	private void save() {
		TabConfig clean = draft.copy();
		byte[] data = TabPayloads.gzip(clean.toJson());
		if (data.length > TabPayloads.MAX_C2S) {
			status(Lang.tr("msg.too_big", data.length / 1024).withColor(0xFF5555));
			return;
		}
		ClientPlayNetworking.send(new TabPayloads.SaveConfig(data));
		original = clean;
		dirty = false;
		status(Lang.tr("msg.sent").withColor(0x55FF55));
	}

	private void status(Component text) {
		status = text;
		statusUntil = Util.getMillis() + 4000;
	}

	@Override
	public void tick() {
		super.tick();
		// Con la vista previa de un color activa no se guarda nada: aún no se ha pulsado OK.
		if (autoApply && dirty && !colorPreviewing && Util.getMillis() - lastEdit > 700) {
			save();
		}
		if (editBurst && Util.getMillis() - lastChangeAt > HISTORY_PAUSE) {
			settleHistory();
		}
	}

	@Override
	public boolean keyPressed(int key, int scanCode, int modifiers) {
		if (popup == Popup.NONE && hasControlDown() && !hasAltDown()
				&& (key == GLFW.GLFW_KEY_Z || key == GLFW.GLFW_KEY_Y)) {
			// Ctrl+Z deshace, Ctrl+Y o Ctrl+Shift+Z rehace (en todo el editor).
			undo(key == GLFW.GLFW_KEY_Y || hasShiftDown());
			return true;
		}
		if (key == GLFW.GLFW_KEY_ESCAPE && popup != Popup.NONE) {
			if (popup == Popup.EFFECT_EDIT) {
				backToEffects();
			} else {
				closePopup(false);
			}
			return true;
		}
		boolean enter = key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER;
		if (inlineBox != null && getFocused() == inlineBox && (enter || key == GLFW.GLFW_KEY_ESCAPE)) {
			inlineLine = -1;
			rebuildWidgets();
			return true;
		}
		if (enter && popup == Popup.COLOR && getFocused() == hexBox) {
			pickColor(pickAlpha << 24 | pickRgb);
			return true;
		}
		return super.keyPressed(key, scanCode, modifiers);
	}

	// ---------------------------------------------------------------------------------------------
	// Ratón

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (popup != Popup.NONE) {
			if (popup == Popup.COLOR && hexBox != null && hexOk != null && hexPreview != null) {
				if (hexBox.mouseClicked(mouseX, mouseY, button)) {
					setFocused(hexBox);
					startTextDrag(hexBox, mouseX);
					return true;
				}
				if (hexOk.mouseClicked(mouseX, mouseY, button) || hexPreview.mouseClicked(mouseX, mouseY, button)) {
					return true;
				}
			}
			if (popup == Popup.EFFECT_EDIT && effectEditorClick(mouseX, mouseY, button)) {
				return true;
			}
			int[] r = popupRect();
			if (mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3]) {
				popupClick((int) mouseX, (int) mouseY);
				return true;
			}
			closePopup(false);
			if (popup != Popup.NONE) {
				return true;
			}
		}
		if (super.mouseClicked(mouseX, mouseY, button)) {
			if (button == 0 && getFocused() instanceof EditBox box && box.isMouseOver(mouseX, mouseY)) {
				startTextDrag(box, mouseX);
			}
			return true;
		}
		int mx = (int) mouseX;
		int my = (int) mouseY;
		if (lineArea != null && lineTarget != null && mx >= lineArea[0] && mx < lineArea[0] + lineArea[2]
				&& my >= lineArea[1] + 2 && my < lineArea[1] + lineArea[3] - 2) {
			int index = (my - lineArea[1] - 2) / 12 + lineScroll;
			if (index >= 0 && index < lineTarget.size()) {
				// Doble clic = editar la línea ahí mismo.
				long now = Util.getMillis();
				boolean twice = index == lastLineClick && now - lastLineClickAt < 400;
				lastLineClick = twice ? -1 : index;
				lastLineClickAt = now;
				selectedLine = index;
				inlineLine = twice ? index : -1;
				click();
				rebuildWidgets();
				return true;
			}
		}
		if (list != null && mx >= list.x() && mx < list.x() + list.w() && my >= list.y() + 2 && my < list.y() + list.h() - 2) {
			int index = (my - list.y() - 2 + listScroll) / 12;
			if (index >= 0 && index < list.items().size()) {
				list.click().accept(index);
				click();
				return true;
			}
		}
		for (Swatch s : swatches) {
			if (mx >= s.x() && mx < s.x() + s.size() && my >= s.y() && my < s.y() + s.size()) {
				// Elegir el color de este campo con la paleta.
				openPopup(Popup.COLOR, hex -> s.field().setValue(hex), s.color().get());
				return true;
			}
		}
		if (tab == Tab.ICONS || tab == Tab.HELP) {
			String snippet = tab == Tab.ICONS ? iconAt(mx, my, iconArea()) : helpAt(mx, my);
			if (snippet != null) {
				if (minecraft != null) {
					minecraft.keyboardHandler.setClipboard(snippet);
				}
				status(Lang.tr("msg.copied", snippet));
				click();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (popup == Popup.EFFECT_EDIT) {
			effectScroll = Math.max(0, effectScroll - (int) Math.signum(scrollY));
			buildEffectEditor();
			return true;
		}
		if (popup != Popup.NONE) {
			popupScroll = Math.max(0, popupScroll - (int) Math.signum(scrollY));
			return true;
		}
		if (lineArea != null && lineTarget != null && mouseX >= lineArea[0] && mouseX < lineArea[0] + lineArea[2]
				&& mouseY >= lineArea[1] && mouseY < lineArea[1] + lineArea[3]) {
			int visible = Math.max(1, (lineArea[3] - 4) / 12);
			lineScroll = Mth.clamp(lineScroll - (int) Math.signum(scrollY), 0, Math.max(0, lineTarget.size() - visible));
			return true;
		}
		if (list != null && mouseX >= list.x() && mouseX < list.x() + list.w() && mouseY >= list.y() && mouseY < list.y() + list.h()) {
			int max = Math.max(0, list.items().size() * 12 - (list.h() - 4));
			listScroll = Mth.clamp(listScroll - (int) (scrollY * 12), 0, max);
			return true;
		}
		if (mouseX < M + leftW()) {
			if (tab == Tab.ICONS || tab == Tab.HELP) {
				popupScroll = Math.max(0, popupScroll - (int) Math.signum(scrollY));
				return true;
			}
			if (rowsMax > 0 && !(getFocused() instanceof MultiLineEditBox)) {
				rowsScroll = Mth.clamp(rowsScroll - (int) Math.signum(scrollY), 0, rowsMax);
				rebuildWidgets();
				return true;
			}
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private void click() {
		if (minecraft != null) {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		}
	}

	// ----- Desplegables

	private static final int SW = 14;
	private static final int SW_STEP = 17;

	private void popupClick(int mx, int my) {
		int[] r = popupRect();
		int x = r[0] + 7;
		int y = r[1] + 17;
		int w = r[2] - 14;
		switch (popup) {
			case COLOR -> {
				int[] sl = alphaSlider();
				if (my >= sl[1] - 2 && my < sl[1] + sl[3] + 2 && mx >= sl[0] - 2 && mx < sl[0] + sl[2] + 2) {
					draggingAlpha = true;
					setAlphaFrom(mx);
					return;
				}
				int i = swatchAt(mx, my);
				if (i >= 0) {
					pickRgb = Palette.COLORS.get(i).rgb();
					syncHex();
					click();
				}
			}
			case ICON -> {
				String s = iconAt(mx, my, new int[] {x, y, w, r[3] - 30});
				if (s != null) {
					if (iconPick != null) {
						// Elegido para el editor de efectos: se vuelve a él.
						iconPick.accept(s);
						iconPick = null;
						click();
						closePopup(false);
					} else {
						insert(s);
					}
				}
			}
			case EFFECT, DATA -> {
				List<PopupRow> rows = popupRows(popup == Popup.EFFECT);
				int i = rowAt(mx, my, x, y, w, r[3] - 42, rows.size());
				if (i >= 0 && rows.get(i).header() == null) {
					PopupRow row = rows.get(i);
					if (row.snippet() != null && row.snippet().editable() && mx >= x + w - EDIT_W) {
						openEffectEditor(row.snippet());
					} else {
						insert(row.insert());
					}
				}
			}
			case EFFECT_EDIT -> effectEditorAreaClick(mx, my);
			default -> {}
		}
	}

	private static final String[] EFFECT_CATEGORIES = {"color", "motion", "animated", "style", "elements", "logic"};
	private static final String[] DATA_CATEGORIES = {"server", "player", "client", "rank"};

	/** Ancho del botón ✎ (editar el efecto) al final de cada fila de efectos. */
	private static final int EDIT_W = 14;
	/** El ratón está sobre un ✎ (lo pone renderRows). */
	private boolean hoverEdit;

	/** Filas de las listas: efectos (nombre | ejemplo animado) o datos ({variable} | valor de ahora). */
	private List<PopupRow> popupRows(boolean effects) {
		List<PopupRow> out = new ArrayList<>();
		if (effects) {
			String sample = Lang.tr("screen.sample").getString();
			for (String cat : EFFECT_CATEGORIES) {
				out.add(new PopupRow(Lang.tr("cat." + cat), "", "", "", Component.empty(), null));
				for (Snippets.Snippet sn : Snippets.EFFECTS) {
					if (sn.category().equals(cat)) {
						out.add(new PopupRow(null, Lang.tr("snippet." + sn.id()).getString(), sn.text(),
								Snippets.example(sn, sample), Lang.tr("snippet." + sn.id() + ".desc"), sn));
					}
				}
			}
		} else {
			for (String cat : DATA_CATEGORIES) {
				// Los datos de Chaos Ranks solo salen si está instalado.
				if (cat.equals("rank") && !chaosRanks) {
					continue;
				}
				out.add(new PopupRow(Lang.tr("cat." + cat), "", "", "", Component.empty(), null));
				for (Snippets.Placeholder ph : Snippets.PLACEHOLDERS) {
					if (ph.category().equals(cat)) {
						String key = "{" + ph.key() + "}";
						out.add(new PopupRow(null, key, key, key, Lang.tr("ph." + ph.key()), null));
					}
				}
			}
			if (!draft.animations.isEmpty()) {
				out.add(new PopupRow(Lang.tr("cat.animations"), "", "", "", Component.empty(), null));
				for (String a : draft.animations.keySet()) {
					String key = "{anim:" + a + "}";
					out.add(new PopupRow(null, key, key, key, Lang.tr("screen.anim_desc"), null));
				}
			}
		}
		return out;
	}

	/** Índice de la fila bajo el ratón en una lista de filas de 12 px con scroll {@link #popupScroll}. */
	private int rowAt(int mx, int my, int x, int y, int w, int h, int size) {
		if (mx < x || mx >= x + w || my < y || my >= y + h) {
			return -1;
		}
		int i = (my - y) / 12 + popupScroll;
		return i < size ? i : -1;
	}

	/**
	 * Dibuja una lista de filas: a la izquierda el nombre o la variable, a la derecha el ejemplo ya pintado (y
	 * animado). Devuelve la fila que tiene el ratón encima.
	 */
	@Nullable
	private PopupRow renderRows(GuiGraphics g, List<PopupRow> rows, int x, int y, int w, int h, int mouseX, int mouseY,
			boolean editButtons) {
		int visible = Math.max(1, h / 12);
		popupScroll = Mth.clamp(popupScroll, 0, Math.max(0, rows.size() - visible));
		ClientContext ctx = new ClientContext(draft, selfValues(), ClientState.global(), ClientContext.now(), previewOpenedAt, false);
		int half = w / 2;
		PopupRow hovered = null;
		hoverEdit = false;
		for (int k = 0; k < visible && k + popupScroll < rows.size(); k++) {
			PopupRow row = rows.get(k + popupScroll);
			int ry = y + k * 12;
			if (row.header() != null) {
				g.drawString(font, row.header(), x, ry + 2, GOLD, false);
				int hw = font.width(row.header());
				g.fill(x + hw + 4, ry + 6, x + w, ry + 7, 0xFF5A3A14);
				continue;
			}
			boolean over = mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + 12;
			if (over) {
				g.fill(x - 2, ry, x + w, ry + 12, ROW_HOVER);
				hovered = row;
			}
			boolean variable = row.label().startsWith("{");
			g.drawString(font, font.plainSubstrByWidth(row.label(), half - 6), x + 4, ry + 2,
					variable ? PLACEHOLDER_TEXT : FIELD_TEXT, false);
			boolean edit = editButtons && row.snippet() != null && row.snippet().editable();
			int exampleEnd = edit ? x + w - EDIT_W - 2 : x + w;
			g.enableScissor(x + half, ry, exampleEnd, ry + 12);
			RichText.draw(g, font, RichText.lines(Evaluator.eval(row.example(), ctx)).get(0), x + half + 2, ry + 2, 1, true);
			g.disableScissor();
			if (edit) {
				// Botón ✎: abrir el editor del efecto.
				int bx = x + w - EDIT_W;
				boolean overEdit = over && mouseX >= bx;
				hoverEdit |= overEdit;
				g.fill(bx, ry + 1, bx + EDIT_W, ry + 11, overEdit ? GOLD : 0xFF5A3A14);
				g.drawString(font, "✎", bx + (EDIT_W - font.width("✎")) / 2 + 1, ry + 2, overEdit ? INK : GOLD_LIGHT, false);
			}
		}
		if (rows.size() > visible) {
			String s = "▲▼ " + (popupScroll + 1) + "/" + (rows.size() - visible + 1);
			g.drawString(font, s, x + w - font.width(s), y - 11, HINT, false);
		}
		return hovered;
	}

	// ----- Editor de un efecto (✎): parámetros y contenido (texto o icono) antes de insertarlo

	private int effectScroll;
	private final List<int[]> popupFields = new ArrayList<>();

	private void openEffectEditor(Snippets.Snippet sn) {
		editing = sn;
		editValues.clear();
		editValues.putAll(sn.defaults());
		editIcon = false;
		editIconTag = "<icon:star>";
		String body = sn.body() == null ? "" : sn.body();
		if (body.contains("text")) {
			// El texto seleccionado en el campo, o la palabra de ejemplo.
			String sel = "";
			if (lastText instanceof EditBox box && children().contains(box)) {
				sel = box.getHighlighted();
			} else if (lastText instanceof MultiLineEditBox area && children().contains(area)) {
				sel = ((MultiLineEditBoxAccessor) area).chaostablist$textField().getSelectedText();
			}
			editText = sel.isEmpty() ? Lang.tr("screen.sample").getString() : sel;
		} else {
			editText = body;
		}
		effectScroll = 0;
		popup = Popup.EFFECT_EDIT;
		click();
		buildEffectEditor();
		updatePopupWidgets();
	}

	/** Vuelve a la lista de efectos sin insertar nada. */
	private void backToEffects() {
		popup = Popup.EFFECT;
		editing = null;
		clearPopupWidgets();
		updatePopupWidgets();
	}

	private void clearPopupWidgets() {
		if (getFocused() instanceof AbstractWidget w && popupWidgets.contains(w)) {
			super.setFocused(lastText != null && children().contains(lastText) ? lastText : null);
		}
		popupWidgets.clear();
		popupSwatches.clear();
		popupLabels.clear();
		popupFields.clear();
	}

	/** El efecto con los valores elegidos. */
	private String buildEffect() {
		if (editing == null) {
			return "";
		}
		String content = editing.body() == null ? null : editIcon ? editIconTag : editText;
		return editing.build(editValues, content);
	}

	private int[] effectRowsArea() {
		int[] r = popupRect();
		return new int[] {r[0] + 7, r[1] + 17, r[2] - 14, r[3] - 17 - 48};
	}

	/** Monta los campos del editor (fuera de la lista de widgets del menú: se dibujan y pulsan a mano). */
	private void buildEffectEditor() {
		clearPopupWidgets();
		Snippets.Snippet sn = editing;
		if (sn == null) {
			return;
		}
		int[] a = effectRowsArea();
		int x = a[0];
		int w = a[2];
		int lw = labelW(w);
		List<RowBuilder> rows = new ArrayList<>();
		for (Snippets.Param p : sn.params()) {
			rows.add((rx, ry, rw) -> paramRow(p, rx, ry, rw, lw));
		}
		if (sn.body() != null) {
			rows.add((rx, ry, rw) -> bodyRow(rx, ry, rw, lw));
		}
		int visible = Math.max(1, a[3] / ROW);
		effectScroll = Mth.clamp(effectScroll, 0, Math.max(0, rows.size() - visible));
		for (int k = 0; k < visible && k + effectScroll < rows.size(); k++) {
			rows.get(k + effectScroll).build(x, a[1] + k * ROW, w);
		}
		if (rows.size() > visible) {
			String s = "▲▼ " + (effectScroll + 1) + "/" + (rows.size() - visible + 1);
			popupLabels.add(new Label(x + w - font.width(s), a[1] - 11, () -> Component.literal(s), HINT, 0));
		}
		if (rows.isEmpty()) {
			popupLabels.add(new Label(x, a[1] + 4, () -> Lang.tr("screen.no_params"), HINT, w));
		}

		// Volver e Insertar.
		int[] r = popupRect();
		int by = r[1] + r[3] - 21;
		Component back = Lang.tr("button.back");
		popupWidgets.add(tip(new GoldButton(x, by, font.width(back) + 12, FH, back, b -> {
			backToEffects();
		}), "button.back.tip"));
		Component ins = Lang.tr("button.insert");
		int iw = font.width(ins) + 12;
		popupWidgets.add(tip(new GoldButton(x + w - iw, by, iw, FH, ins, b -> insert(buildEffect(), false, true)),
				"button.insert.tip"));
		Component reset = Lang.tr("button.reset");
		int rw = font.width(reset) + 12;
		popupWidgets.add(tip(new GoldButton(x + w - iw - 2 - rw, by, rw, FH, reset, b -> {
			editValues.clear();
			editValues.putAll(sn.defaults());
			buildEffectEditor();
		}), "button.reset.tip"));
	}

	private EditBox popupField(int x, int y, int w, int max, String value, Consumer<String> onChange) {
		popupFields.add(new int[] {x, y, w, FH});
		EditBox box = new EditBox(font, x + 4, y + 4, w - 8, 10, Component.empty());
		box.setBordered(false);
		box.setTextColor(FIELD_TEXT);
		box.setMaxLength(max);
		box.setValue(value);
		box.moveCursorToStart(false);
		box.setResponder(onChange);
		popupWidgets.add(box);
		return box;
	}

	private void paramRow(Snippets.Param p, int x, int y, int w, int lw) {
		Component name = Lang.tr("param." + p.key());
		popupLabels.add(new Label(x, y + 4, () -> name, LABEL, -(lw - 4)));
		String key = p.key();
		String value = editValues.getOrDefault(key, p.def());
		int fx = x + lw;
		int fw = w - lw;
		switch (p.kind()) {
			case COLOR -> {
				EditBox box = popupField(fx, y, fw - 18, 9, value, v -> {
					if (ColorUtil.parse(v) != null) {
						editValues.put(key, v.toUpperCase());
					}
				});
				popupSwatches.add(new Swatch(x + w - 15, y + 1, 14, () -> editValues.getOrDefault(key, p.def()), box));
			}
			case NUMBER -> {
				EditBox box = popupField(fx + 16, y, fw - 32, 12, value, v -> {
					String clean = v.trim().replace(',', '.');
					if (clean.matches("-?[0-9]+(\\.[0-9]+)?")) {
						editValues.put(key, clean);
					}
				});
				popupWidgets.add(new GoldButton(fx, y, 14, FH, Component.literal("-"), b -> {
					box.setValue(step(p, editValues.getOrDefault(key, p.def()), -1));
				}));
				popupWidgets.add(new GoldButton(x + w - 14, y, 14, FH, Component.literal("+"), b -> {
					box.setValue(step(p, editValues.getOrDefault(key, p.def()), 1));
				}));
			}
			case TEXT -> popupField(fx, y, fw, 256, value, v -> editValues.put(key, v));
			case BOOL, CHOICE -> {
				Component text = p.kind() == Snippets.Kind.BOOL ? Lang.yesNo("true".equals(value)) : Component.literal(value);
				popupWidgets.add(new GoldButton(fx, y, fw, FH, text, b -> {
					List<String> opts = p.options();
					int i = Math.max(0, opts.indexOf(editValues.getOrDefault(key, p.def())));
					editValues.put(key, opts.get((i + 1) % opts.size()));
					buildEffectEditor();
				}));
			}
		}
	}

	private static String step(Snippets.Param p, String current, int dir) {
		double v;
		try {
			v = Double.parseDouble(current);
		} catch (NumberFormatException e) {
			v = Double.parseDouble(p.def());
		}
		return Snippets.format(Mth.clamp(v + dir * p.step(), p.min(), p.max()));
	}

	/** Contenido del efecto: un texto escrito o un icono. */
	private void bodyRow(int x, int y, int w, int lw) {
		popupLabels.add(new Label(x, y + 4, () -> Lang.tr("param.body"), LABEL, -(lw - 4)));
		Component mode = Lang.tr(editIcon ? "body.icon" : "body.text");
		int mw = Math.max(font.width(Lang.tr("body.icon")), font.width(Lang.tr("body.text"))) + 12;
		popupWidgets.add(tip(new GoldButton(x + lw, y, mw, FH, mode, b -> {
			editIcon = !editIcon;
			buildEffectEditor();
		}), "body.tip"));
		int fx = x + lw + mw + 2;
		int fw = x + w - fx;
		if (editIcon) {
			popupWidgets.add(tip(new GoldButton(fx, y, fw, FH, Lang.tr("button.pick_icon"), b -> {
				openPopup(Popup.ICON, null);
				iconPick = s -> editIconTag = s;
				returnTo = Popup.EFFECT_EDIT;
				updatePopupWidgets();
			}), "button.pick_icon.tip"));
		} else {
			popupField(fx, y, fw, 256, editText, v -> editText = v);
		}
	}

	private boolean effectEditorClick(double mouseX, double mouseY, int button) {
		for (AbstractWidget w : new ArrayList<>(popupWidgets)) {
			if (w.visible && w.mouseClicked(mouseX, mouseY, button)) {
				if (w instanceof EditBox box) {
					setFocused(box);
					startTextDrag(box, mouseX);
				}
				return true;
			}
		}
		for (Swatch s : popupSwatches) {
			if (mouseX >= s.x() && mouseX < s.x() + s.size() && mouseY >= s.y() && mouseY < s.y() + s.size()) {
				openPopup(Popup.COLOR, hex -> s.field().setValue(hex), s.color().get());
				returnTo = Popup.EFFECT_EDIT;
				updatePopupWidgets();
				return true;
			}
		}
		return false;
	}

	private void effectEditorAreaClick(int mx, int my) {
		// Clic en una zona vacía del editor: se quita el foco de los campos.
		if (getFocused() instanceof AbstractWidget w && popupWidgets.contains(w)) {
			w.setFocused(false);
		}
	}

	private void renderEffectEditor(GuiGraphics g, int[] r, int mouseX, int mouseY) {
		for (int[] f : popupFields) {
			g.blitSprite(INSET, f[0], f[1], f[2], f[3]);
		}
		for (Label l : popupLabels) {
			drawLabel(g, l);
		}
		for (AbstractWidget w : popupWidgets) {
			w.render(g, mouseX, mouseY, 0);
		}
		for (Swatch s : popupSwatches) {
			int c = ColorUtil.parse(s.color().get(), 0);
			g.fill(s.x() - 1, s.y() - 1, s.x() + s.size() + 1, s.y() + s.size() + 1, INK);
			checker(g, s.x(), s.y(), s.size());
			g.fill(s.x(), s.y(), s.x() + s.size(), s.y() + s.size(), c);
		}
		// Cómo queda y la etiqueta que se va a insertar.
		int x = r[0] + 7;
		int w = r[2] - 14;
		int py = r[1] + r[3] - 44;
		g.fill(x, py - 3, x + w, py - 2, 0xFF5A3A14);
		Component looks = Lang.tr("screen.looks_like");
		g.drawString(font, looks, x, py + 1, HINT, false);
		int lx = x + font.width(looks) + 4;
		String built = buildEffect();
		ClientContext ctx = new ClientContext(draft, selfValues(), ClientState.global(), ClientContext.now(), previewOpenedAt, false);
		g.enableScissor(lx, py - 2, x + w, py + 11);
		RichText.draw(g, font, RichText.lines(Evaluator.eval(built, ctx)).get(0), lx, py + 1, 1, true);
		g.disableScissor();
		g.drawString(font, font.plainSubstrByWidth(built, w), x, py + 12, TAG_TEXT, false);
	}

	// ----- Selector de color

	private static final int SWC = 12;
	private static final int SWC_STEP = 14;

	private int swatchCols() {
		return Math.max(1, (popupRect()[2] - 14) / SWC_STEP);
	}

	/** Filas de la rejilla de colores que caben (encima de la transparencia y del campo hex). */
	private int swatchRows() {
		return Math.max(1, (popupRect()[3] - 17 - 44) / SWC_STEP);
	}

	private int swatchAt(int mx, int my) {
		int[] r = popupRect();
		int x = r[0] + 7;
		int y = r[1] + 17;
		int col = (mx - x) / SWC_STEP;
		int row = (my - y) / SWC_STEP;
		if (mx < x || my < y || col >= swatchCols() || row >= swatchRows()) {
			return -1;
		}
		int i = (row + popupScroll) * swatchCols() + col;
		return i < Palette.COLORS.size() ? i : -1;
	}

	/** Barra de transparencia: {x, y, ancho, alto}. */
	private int[] alphaSlider() {
		int[] r = popupRect();
		int x = r[0] + 7 + font.width(Lang.tr("screen.opacity")) + 4;
		return new int[] {x, r[1] + r[3] - 37, r[0] + r[2] - 7 - 30 - x, 9};
	}

	private void setAlphaFrom(double mx) {
		int[] sl = alphaSlider();
		pickAlpha = Mth.clamp((int) Math.round((mx - sl[0]) / sl[2] * 255), 0, 255);
		syncHex();
	}

	/** Pone en el campo hex el color elegido (#RRGGBB si es opaco, #AARRGGBB si no). */
	private void syncHex() {
		if (hexBox == null) {
			return;
		}
		syncingHex = true;
		hexBox.setValue(pickAlpha == 255 ? String.format("#%06X", pickRgb) : String.format("#%02X%06X", pickAlpha, pickRgb));
		syncingHex = false;
		refreshColorPreview();
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (draggingAlpha) {
			setAlphaFrom(mouseX);
			return true;
		}
		if (dragBox != null && button == 0) {
			// Seleccionar arrastrando: el inicio se queda donde se hizo clic.
			dragBox.moveCursorTo(textIndexAt(dragBox, mouseX), true);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		draggingAlpha = false;
		dragBox = null;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	// ----- Seleccionar texto con el ratón en los campos de una línea

	@Nullable private EditBox dragBox;
	@Nullable private EditBox lastClickBox;
	private long lastClickBoxAt;

	/** Tras un clic en un campo: empieza a arrastrar; doble clic selecciona la palabra. */
	private void startTextDrag(EditBox box, double mouseX) {
		dragBox = box;
		long now = Util.getMillis();
		if (box == lastClickBox && now - lastClickBoxAt < 350) {
			selectWord(box);
			dragBox = null;
			lastClickBox = null;
			return;
		}
		lastClickBox = box;
		lastClickBoxAt = now;
	}

	/** Posición del texto bajo la x del ratón (fuera del campo, un carácter más allá para desplazarlo). */
	private int textIndexAt(EditBox box, double mouseX) {
		String value = box.getValue();
		int from = Mth.clamp(((EditBoxAccessor) box).chaostablist$displayPos(), 0, value.length());
		int rel = (int) mouseX - box.getX() - (box.isBordered() ? 4 : 0);
		if (rel < 0) {
			return Math.max(0, from - 1);
		}
		String visible = font.plainSubstrByWidth(value.substring(from), box.getInnerWidth());
		if (rel > box.getInnerWidth()) {
			return Math.min(value.length(), from + visible.length() + 1);
		}
		return from + font.plainSubstrByWidth(visible, rel).length();
	}

	/** Selecciona la palabra (o la etiqueta &lt;...&gt; / el dato {...}) donde está el cursor. */
	private static void selectWord(EditBox box) {
		String v = box.getValue();
		int c = Mth.clamp(box.getCursorPosition(), 0, v.length());
		int open = v.lastIndexOf('<', Math.max(0, c - 1));
		int close = v.indexOf('>', Math.max(0, c - 1));
		int lastClose = v.lastIndexOf('>', Math.max(0, c - 1));
		if (open >= 0 && close >= 0 && (lastClose < open || lastClose == close) && open < c && c <= close + 1) {
			box.moveCursorTo(open, false);
			box.moveCursorTo(close + 1, true);
			return;
		}
		int start = c;
		while (start > 0 && isWordChar(v.charAt(start - 1))) {
			start--;
		}
		int end = c;
		while (end < v.length() && isWordChar(v.charAt(end))) {
			end++;
		}
		if (start == end) {
			return;
		}
		box.moveCursorTo(start, false);
		box.moveCursorTo(end, true);
	}

	private static boolean isWordChar(char ch) {
		return Character.isLetterOrDigit(ch) || ch == '_' || ch == '#' || ch == '{' || ch == '}';
	}

	/** Celdas de iconos: los de la fuente y luego los PNG del servidor. */
	private List<String> iconIds() {
		List<String> ids = new ArrayList<>();
		for (Icons.Icon i : Icons.icons()) {
			ids.add(i.id());
		}
		for (String c : CustomIcons.names()) {
			ids.add("custom/" + c);
		}
		return ids;
	}

	private int[] iconArea() {
		int[] c = content();
		return new int[] {c[0], c[1] + 12, c[2], c[3] - 24};
	}

	@Nullable
	private String iconAt(int mx, int my, int[] area) {
		int cell = 18;
		int cols = Math.max(1, area[2] / cell);
		int rows = Math.max(1, area[3] / cell);
		int col = (mx - area[0]) / cell;
		int row = (my - area[1]) / cell;
		if (mx < area[0] || my < area[1] || col >= cols || row >= rows) {
			return null;
		}
		List<String> ids = iconIds();
		int i = (row + popupScroll) * cols + col;
		return i < ids.size() ? "<icon:" + ids.get(i) + ">" : null;
	}

	// ---------------------------------------------------------------------------------------------
	// Dibujo

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderTransparentBackground(g);
		g.blitSprite(PANEL, 0, 0, width, height);
		g.blitSprite(INSET, M, contentTop(), leftW(), bottomY() - 3 - contentTop());
		g.blitSprite(INSET, previewX(), contentTop(), previewW(), bottomY() - 3 - contentTop());
		for (int[] f : fields) {
			g.blitSprite(INSET, f[0], f[1], f[2], f[3]);
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		renderTitle(g);
		for (Label l : labels) {
			drawLabel(g, l);
		}
		for (Swatch s : swatches) {
			int c = ColorUtil.parse(s.color().get(), 0);
			g.fill(s.x() - 1, s.y() - 1, s.x() + s.size() + 1, s.y() + s.size() + 1, INK);
			checker(g, s.x(), s.y(), s.size());
			g.fill(s.x(), s.y(), s.x() + s.size(), s.y() + s.size(), c);
		}
		if (scrollInfo != null) {
			String s = "▲▼ " + (scrollInfo[2] + 1) + "/" + (scrollInfo[3] + 1);
			g.drawString(font, s, scrollInfo[0] - font.width(s), scrollInfo[1] + 2, HINT, false);
		}
		if (list != null) {
			renderList(g, mouseX, mouseY);
		}
		if (lineArea != null && lineTarget != null) {
			renderLines(g, mouseX, mouseY);
		}
		for (Preview p : previews) {
			ClientContext ctx = new ClientContext(draft, p.values().get(), ClientState.global(), ClientContext.now(),
					previewOpenedAt, false);
			g.enableScissor(p.x(), p.y() - 2, p.x() + p.w(), p.y() + 10);
			RichText.draw(g, font, RichText.lines(Evaluator.eval(p.template().get(), ctx)).get(0), p.x(), p.y(), 1, true);
			g.disableScissor();
		}
		switch (tab) {
			case ICONS -> renderIconsTab(g, mouseX, mouseY);
			case HELP -> renderHelp(g, mouseX, mouseY);
			default -> {}
		}
		renderPreview(g);
		if (popup != Popup.NONE) {
			renderPopup(g, mouseX, mouseY);
		}
	}

	/** Etiquetas: maxWidth &gt; 0 = partir en líneas; &lt; 0 = recortar a una línea. */
	private void drawLabel(GuiGraphics g, Label l) {
		Component text = l.text().get();
		if (l.maxWidth() > 0) {
			int y = l.y();
			for (FormattedCharSequence line : font.split(text, l.maxWidth())) {
				g.drawString(font, line, l.x(), y, l.color(), false);
				y += 10;
			}
		} else if (l.maxWidth() < 0 && font.width(text) > -l.maxWidth()) {
			String cut = font.plainSubstrByWidth(text.getString(), -l.maxWidth() - font.width("…")) + "…";
			g.drawString(font, cut, l.x(), l.y(), l.color(), false);
		} else {
			g.drawString(font, text, l.x(), l.y(), l.color(), false);
		}
	}

	private void renderTitle(GuiGraphics g) {
		List<Glyph> badge = new ArrayList<>();
		Badges.build(badge, 0, Icons.FONT, "CHAOS TABLIST", 0xE8B23A, 0x2A1606, "players", 0x2A1606, 0x5A3A08, false);
		RichText.draw(g, font, badge, M + 2, 7, 1, false);
		int x = M + 8 + RichText.width(font, badge);
		int room = width - M - x;
		Component text;
		int color;
		if (Util.getMillis() < statusUntil) {
			text = status;
			color = 0xFFFFFF;
		} else if (dirty) {
			text = Lang.tr("screen.unsaved");
			color = 0xFFAA00;
		} else {
			text = Lang.tr("screen.subtitle");
			color = HINT;
		}
		String s = font.plainSubstrByWidth(text.getString(), room);
		int tx = width - M - font.width(s);
		g.drawString(font, Component.literal(s).withStyle(text.getStyle()), Math.max(x, tx), 7, color, false);
	}

	private void renderList(GuiGraphics g, int mouseX, int mouseY) {
		ListArea l = list;
		g.blitSprite(INSET, l.x(), l.y(), l.w(), l.h());
		g.enableScissor(l.x() + 2, l.y() + 2, l.x() + l.w() - 2, l.y() + l.h() - 2);
		for (int i = 0; i < l.items().size(); i++) {
			int ry = l.y() + 2 + i * 12 - listScroll;
			boolean hovered = mouseX >= l.x() && mouseX < l.x() + l.w() && mouseY >= ry && mouseY < ry + 12;
			if (i == l.selected()) {
				g.fill(l.x() + 2, ry, l.x() + l.w() - 2, ry + 12, ROW_SELECTED);
				g.fill(l.x() + 2, ry, l.x() + 4, ry + 12, GOLD);
			} else if (hovered) {
				g.fill(l.x() + 2, ry, l.x() + l.w() - 2, ry + 12, ROW_HOVER);
			}
			g.drawString(font, l.items().get(i), l.x() + 6, ry + 2, 0xFFFFFF, false);
		}
		g.disableScissor();
	}

	/** Las líneas del editor visual, ya pintadas; la elegida resaltada. */
	private void renderLines(GuiGraphics g, int mouseX, int mouseY) {
		int[] a = lineArea;
		List<String> lines = lineTarget;
		g.blitSprite(INSET, a[0], a[1], a[2], a[3]);
		int visible = Math.max(1, (a[3] - 4) / 12);
		if (selectedLine < lineScroll) {
			lineScroll = selectedLine;
		} else if (selectedLine >= lineScroll + visible) {
			lineScroll = selectedLine - visible + 1;
		}
		lineScroll = Mth.clamp(lineScroll, 0, Math.max(0, lines.size() - visible));
		ClientContext ctx = new ClientContext(draft, selfValues(), ClientState.global(), ClientContext.now(), previewOpenedAt, false);
		g.enableScissor(a[0] + 2, a[1] + 2, a[0] + a[2] - 2, a[1] + a[3] - 2);
		for (int k = 0; k < visible && k + lineScroll < lines.size(); k++) {
			int i = k + lineScroll;
			int ry = a[1] + 2 + k * 12;
			boolean over = mouseX >= a[0] && mouseX < a[0] + a[2] && mouseY >= ry && mouseY < ry + 12;
			if (i == selectedLine) {
				g.fill(a[0] + 2, ry, a[0] + a[2] - 2, ry + 12, ROW_SELECTED);
				g.fill(a[0] + 2, ry, a[0] + 4, ry + 12, GOLD);
			} else if (over) {
				g.fill(a[0] + 2, ry, a[0] + a[2] - 2, ry + 12, ROW_HOVER);
			}
			String num = String.valueOf(i + 1);
			g.drawString(font, num, a[0] + 16 - font.width(num), ry + 2, HINT, false);
			String line = lines.get(i);
			if (i == inlineLine && inlineBox != null) {
				// Se está editando aquí: el campo se dibuja encima.
				g.fill(a[0] + 18, ry, a[0] + a[2] - 2, ry + 12, 0xFF1A0F08);
				g.fill(a[0] + 18, ry + 11, a[0] + a[2] - 2, ry + 12, GOLD);
			} else if (line.isBlank()) {
				g.drawString(font, Lang.tr("screen.empty_line"), a[0] + 20, ry + 2, 0x6A5A46, false);
			} else {
				RichText.draw(g, font, RichText.lines(Evaluator.eval(line, ctx)).get(0), a[0] + 20, ry + 2, 1, true);
			}
		}
		g.disableScissor();
		if (inlineBox != null) {
			inlineBox.render(g, mouseX, mouseY, 0);
		}
		// Explicación al dejar el ratón un rato sobre la lista.
		boolean overList = popup == Popup.NONE && mouseX >= a[0] && mouseX < a[0] + a[2] && mouseY >= a[1] && mouseY < a[1] + a[3];
		if (!overList) {
			linesHoverSince = 0;
		} else if (linesHoverSince == 0) {
			linesHoverSince = Util.getMillis();
		} else if (Util.getMillis() - linesHoverSince > 700 && inlineBox == null) {
			setTooltipForNextRenderPass(Lang.tr("screen.lines.tip"));
		}
	}

	private void renderPreview(GuiGraphics g) {
		int px = previewX();
		int py = contentTop();
		int pw = previewW();
		int bottom = bottomY() - 4 - FH - 5;
		Component title = Lang.tr(vanillaPreview ? "screen.preview_vanilla" : "screen.preview");
		g.drawString(font, font.plainSubstrByWidth(title.getString(), pw - 10), px + 5, py + 5, LABEL, false);
		float progress = 1;
		if (previewAnimStart > 0) {
			progress = Mth.clamp((Util.getMillis() - previewAnimStart) / (float) Math.max(1, draft.layout.openMillis), 0, 1);
		}
		g.enableScissor(px + 2, py + 15, px + pw - 2, bottom);
		try {
			var board = minecraft != null && minecraft.level != null ? minecraft.level.getScoreboard() : null;
			var objective = board == null ? null : board.getDisplayObjective(DisplaySlot.LIST);
			TabRenderer.render(g, font, draft, previewEntries(), ClientState.global(), px + pw / 2, py + 17, pw - 8,
					bottom - py - 19, objective, board, ClientContext.now(), previewOpenedAt, progress, vanillaPreview);
		} catch (RuntimeException e) {
			g.drawString(font, Component.literal("Error: " + e.getMessage()), px + 6, py + 20, 0xFF5555, false);
		}
		g.disableScissor();
	}

	/** Jugadores reales y de ejemplo. */
	private List<TabRenderer.Entry> previewEntries() {
		List<TabRenderer.Entry> out = new ArrayList<>();
		if (minecraft != null && minecraft.getConnection() != null) {
			out.addAll(TabRenderer.entries(minecraft, draft));
		}
		out.addAll(TabRenderer.fakes(draft, fakePlayers));
		out.sort(TabRenderer.comparator(draft));
		return out;
	}

	private String playerName() {
		return minecraft != null && minecraft.player != null ? minecraft.player.getScoreboardName() : "Steve";
	}

	private void renderIconsTab(GuiGraphics g, int mouseX, int mouseY) {
		int[] c = content();
		drawLabel(g, new Label(c[0], c[1], () -> Lang.tr("screen.icons_hint"), LABEL, -c[2]));
		String hovered = renderIconGrid(g, iconArea(), mouseX, mouseY);
		if (hovered != null) {
			drawLabel(g, new Label(c[0], c[1] + c[3] - 9, () -> Component.literal(hovered), HINT, -c[2]));
		}
	}

	/** Dibuja la rejilla de iconos y devuelve el texto del que tiene el ratón encima. */
	@Nullable
	private String renderIconGrid(GuiGraphics g, int[] area, int mouseX, int mouseY) {
		int cell = 18;
		int cols = Math.max(1, area[2] / cell);
		int rows = Math.max(1, area[3] / cell);
		List<String> ids = iconIds();
		int maxScroll = Math.max(0, (ids.size() + cols - 1) / cols - rows);
		popupScroll = Math.min(popupScroll, maxScroll);
		String hovered = null;
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				int i = (r + popupScroll) * cols + c;
				if (i >= ids.size()) {
					continue;
				}
				String id = ids.get(i);
				int cx = area[0] + c * cell;
				int cy = area[1] + r * cell;
				g.blitSprite(CELL, cx, cy, cell - 2, cell - 2);
				boolean over = mouseX >= cx && mouseX < cx + cell - 2 && mouseY >= cy && mouseY < cy + cell - 2;
				if (over) {
					g.fill(cx + 1, cy + 1, cx + cell - 3, cy + cell - 3, 0x40FFE08A);
					Icons.Icon icon = Icons.icon(id);
					hovered = "<icon:" + id + ">" + (icon != null ? "   vanilla: " + icon.fallback() : "");
				}
				if (id.startsWith("custom/")) {
					CustomIcons.Icon ci = CustomIcons.get(id.substring(7));
					if (ci != null) {
						g.blit(ci.texture(), cx + 1, cy + 1, 14, 14, 0, 0, ci.width(), ci.height(), ci.width(), ci.height());
					}
				} else {
					Icons.Icon icon = Icons.icon(id);
					if (icon != null) {
						g.pose().pushPose();
						g.pose().translate(cx + 1, cy + 1, 0);
						g.pose().scale(2, 2, 1);
						g.drawString(font, Component.literal(String.valueOf(icon.glyph()))
								.withStyle(Style.EMPTY.withFont(Icons.FONT_COMPACT)), 0, 0, 0xFFEBC0, false);
						g.pose().popPose();
					}
				}
			}
		}
		return hovered;
	}

	private void renderHelp(GuiGraphics g, int mouseX, int mouseY) {
		int[] c = content();
		drawLabel(g, new Label(c[0], c[1], () -> Lang.tr("screen.help_title"), LABEL, -(c[2] - 60)));
		PopupRow hovered = renderRows(g, helpRows(), c[0], c[1] + 12, c[2], c[3] - 34, mouseX, mouseY, false);
		if (hovered != null) {
			int fy = c[1] + c[3] - 20;
			g.drawString(font, font.plainSubstrByWidth(hovered.desc().getString(), c[2]), c[0], fy, LABEL, false);
			g.drawString(font, font.plainSubstrByWidth(hovered.insert(), c[2]), c[0], fy + 10, HINT, false);
		}
	}

	/** Ayuda: todos los datos y luego todos los efectos. */
	private List<PopupRow> helpRows() {
		List<PopupRow> rows = new ArrayList<>(popupRows(false));
		rows.addAll(popupRows(true));
		return rows;
	}

	@Nullable
	private String helpAt(int mx, int my) {
		int[] c = content();
		List<PopupRow> rows = helpRows();
		int i = rowAt(mx, my, c[0], c[1] + 12, c[2], c[3] - 34, rows.size());
		return i >= 0 && rows.get(i).header() == null ? rows.get(i).insert() : null;
	}

	private Map<String, String> selfValues() {
		if (minecraft != null && minecraft.player != null) {
			Map<String, String> v = new HashMap<>(ClientState.player(minecraft.player.getUUID()));
			v.putIfAbsent("player", playerName());
			return v;
		}
		return Map.of("player", "Steve");
	}

	private void renderPopup(GuiGraphics g, int mouseX, int mouseY) {
		int[] r = popupRect();
		g.pose().pushPose();
		g.pose().translate(0, 0, 400);
		g.blitSprite(PANEL, r[0], r[1], r[2], r[3]);
		Component title = popup == Popup.EFFECT_EDIT && editing != null
				? Lang.tr("popup.effect_edit", Lang.tr("snippet." + editing.id()))
				: iconPick != null ? Lang.tr("popup.icon_pick") : Lang.tr("popup." + popup.name().toLowerCase());
		g.drawString(font, font.plainSubstrByWidth(title.getString(), r[2] - 70), r[0] + 7, r[1] + 6, LABEL, false);
		int x = r[0] + 7;
		int y = r[1] + 17;
		int w = r[2] - 14;
		int footer = r[1] + r[3] - 24;
		switch (popup) {
			case COLOR -> renderColorPicker(g, r, mouseX, mouseY);
			case ICON -> {
				String hovered = renderIconGrid(g, new int[] {x, y, w, r[3] - 30}, mouseX, mouseY);
				if (hovered != null) {
					g.drawString(font, font.plainSubstrByWidth(hovered, w), x, footer + 12, HINT, false);
				}
			}
			case EFFECT, DATA -> {
				PopupRow hovered = renderRows(g, popupRows(popup == Popup.EFFECT), x, y, w, r[3] - 42, mouseX, mouseY,
						popup == Popup.EFFECT);
				if (hovered != null) {
					// Qué hace y lo que se va a insertar (o qué hace el ✎).
					Component desc = hoverEdit ? Lang.tr("screen.edit_effect_hint") : hovered.desc();
					g.drawString(font, font.plainSubstrByWidth(desc.getString(), w), x, footer + 1, LABEL, false);
					g.drawString(font, font.plainSubstrByWidth(hovered.insert(), w), x, footer + 12, HINT, false);
				} else {
					g.drawString(font, font.plainSubstrByWidth(Lang.tr("screen.popup_hint").getString(), w), x, footer + 6, HINT, false);
				}
			}
			case EFFECT_EDIT -> renderEffectEditor(g, r, mouseX, mouseY);
			default -> {}
		}
		g.pose().popPose();
		if (popup == Popup.COLOR && hexBox != null && hexOk != null && hexPreview != null) {
			g.pose().pushPose();
			g.pose().translate(0, 0, 450);
			g.blitSprite(INSET, hexBox.getX() - 4, hexBox.getY() - 4, hexBox.getWidth() + 8, FH);
			hexBox.render(g, mouseX, mouseY, 0);
			hexPreview.render(g, mouseX, mouseY, 0);
			hexOk.render(g, mouseX, mouseY, 0);
			if (colorPreviewing) {
				// Marca de que lo que se ve en la vista previa aún no está aceptado.
				g.fill(hexPreview.getX(), hexPreview.getY() + FH, hexPreview.getX() + hexPreview.getWidth(), hexPreview.getY() + FH + 1,
						0xFF55FF55);
			}
			g.pose().popPose();
		}
	}

	private void renderColorPicker(GuiGraphics g, int[] r, int mouseX, int mouseY) {
		int x = r[0] + 7;
		int y = r[1] + 17;
		int cols = swatchCols();
		int rows = swatchRows();
		int maxScroll = Math.max(0, (Palette.COLORS.size() + cols - 1) / cols - rows);
		popupScroll = Mth.clamp(popupScroll, 0, maxScroll);
		Palette.Color hovered = null;
		for (int k = 0; k < rows * cols; k++) {
			int i = popupScroll * cols + k;
			if (i >= Palette.COLORS.size()) {
				break;
			}
			Palette.Color c = Palette.COLORS.get(i);
			int cx = x + (k % cols) * SWC_STEP;
			int cy = y + (k / cols) * SWC_STEP;
			boolean over = mouseX >= cx && mouseX < cx + SWC && mouseY >= cy && mouseY < cy + SWC;
			boolean chosen = c.rgb() == pickRgb;
			g.fill(cx - 1, cy - 1, cx + SWC + 1, cy + SWC + 1, chosen ? GOLD_LIGHT : over ? GOLD : INK);
			g.fill(cx, cy, cx + SWC, cy + SWC, 0xFF000000 | c.rgb());
			g.fill(cx, cy, cx + SWC, cy + 1, 0x40FFFFFF);
			if (over) {
				hovered = c;
			}
		}
		if (maxScroll > 0) {
			String s = "▲▼";
			g.drawString(font, s, r[0] + r[2] - 7 - font.width(s), r[1] + 6, HINT, false);
		}

		// Transparencia: de transparente (izquierda) a opaco (derecha), sobre cuadros.
		int[] sl = alphaSlider();
		g.drawString(font, Lang.tr("screen.opacity"), x, sl[1] + 1, LABEL, false);
		g.fill(sl[0] - 1, sl[1] - 1, sl[0] + sl[2] + 1, sl[1] + sl[3] + 1, INK);
		checkerRect(g, sl[0], sl[1], sl[2], sl[3]);
		for (int i = 0; i < sl[2]; i++) {
			int a = Math.round(i / (float) Math.max(1, sl[2] - 1) * 255);
			g.fill(sl[0] + i, sl[1], sl[0] + i + 1, sl[1] + sl[3], a << 24 | pickRgb);
		}
		int knob = sl[0] + Math.round(pickAlpha / 255f * (sl[2] - 1));
		g.fill(knob - 1, sl[1] - 2, knob + 2, sl[1] + sl[3] + 2, 0xFFFFFFFF);
		g.fill(knob, sl[1] - 1, knob + 1, sl[1] + sl[3] + 1, 0xFF000000);
		String pct = Math.round(pickAlpha / 2.55f) + "%";
		g.drawString(font, pct, r[0] + r[2] - 7 - font.width(pct), sl[1] + 1, 0xFFFFFF, false);

		// Muestra grande del color con su transparencia.
		int sy = r[1] + r[3] - 21;
		g.fill(x - 1, sy - 1, x + 31, sy + FH + 1, INK);
		checkerRect(g, x, sy, 30, FH);
		g.fill(x, sy, x + 30, sy + FH, pickAlpha << 24 | pickRgb);
		if (hovered != null) {
			Component name = hovered.name().copy().append(String.format("  #%06X", hovered.rgb()));
			g.drawString(font, font.plainSubstrByWidth(name.getString(), r[2] - 14), x, r[1] + 6, HINT, false);
		}
	}

	private static void checkerRect(GuiGraphics g, int x, int y, int w, int h) {
		for (int i = 0; i < w; i += 4) {
			for (int j = 0; j < h; j += 4) {
				g.fill(x + i, y + j, x + Math.min(w, i + 4), y + Math.min(h, j + 4),
						((i + j) / 4) % 2 == 0 ? 0xFF9A9A9A : 0xFF5A5A5A);
			}
		}
	}

	private static void checker(GuiGraphics g, int x, int y, int size) {
		for (int i = 0; i < size; i += 4) {
			for (int j = 0; j < size; j += 4) {
				g.fill(x + i, y + j, x + Math.min(size, i + 4), y + Math.min(size, j + 4),
						((i + j) / 4) % 2 == 0 ? 0xFF9A9A9A : 0xFF5A5A5A);
			}
		}
	}

	/** Colorea las plantillas mientras se escriben: etiquetas en morado y placeholders en celeste. */
	private static FormattedCharSequence highlight(String full, int offset, String visible) {
		boolean inTag = false;
		boolean inPh = false;
		for (int i = 0; i < Math.min(offset, full.length()); i++) {
			char c = full.charAt(i);
			if (c == '<') {
				inTag = true;
			} else if (c == '>') {
				inTag = false;
			} else if (c == '{') {
				inPh = true;
			} else if (c == '}') {
				inPh = false;
			}
		}
		List<FormattedCharSequence> parts = new ArrayList<>();
		StringBuilder run = new StringBuilder();
		int runColor = -1;
		for (int i = 0; i < visible.length(); i++) {
			char c = visible.charAt(i);
			if (c == '<') {
				inTag = true;
			}
			if (c == '{') {
				inPh = true;
			}
			int color = inTag ? TAG_TEXT : inPh ? PLACEHOLDER_TEXT : FIELD_TEXT;
			if (c == '>') {
				inTag = false;
			}
			if (c == '}') {
				inPh = false;
			}
			if (color != runColor && !run.isEmpty()) {
				parts.add(FormattedCharSequence.forward(run.toString(), Style.EMPTY.withColor(runColor)));
				run.setLength(0);
			}
			runColor = color;
			run.append(c);
		}
		if (!run.isEmpty()) {
			parts.add(FormattedCharSequence.forward(run.toString(), Style.EMPTY.withColor(runColor)));
		}
		return FormattedCharSequence.composite(parts);
	}

	// ---------------------------------------------------------------------------------------------
	// Autoprueba

	public void showTab(int index) {
		tab = Tab.values()[index];
		popup = Popup.NONE;
		rowsScroll = 0;
		rebuildWidgets();
	}

	public void setVanillaPreview(boolean vanilla) {
		vanillaPreview = vanilla;
		rebuildWidgets();
	}

	public TabConfig draft() {
		return draft;
	}

	/** Como pulsar "Guardar". */
	public void saveForTest() {
		save();
	}

	public void openPopupForTest(int index) {
		openPopup(Popup.values()[index], null);
	}

	/** Abre el editor (✎) de un efecto. */
	public void openEffectEditorForTest(String id) {
		Snippets.Snippet sn = Snippets.effect(id);
		if (sn != null) {
			openEffectEditor(sn);
		}
	}

	/** Paleta para el color del panel, con la vista previa activa (sin pulsar OK). */
	public void colorPreviewForTest(int rgb) {
		TabConfig.Layout l = draft.layout;
		openPopup(Popup.COLOR, v -> l.panelColor = v, l.panelColor);
		pickRgb = rgb;
		syncHex();
		toggleColorPreview();
	}

	/** Cierra el desplegable sin aceptar (la vista previa se deshace). */
	public void closePopupForTest() {
		closePopup();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
