package com.chaos.tablist.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.chaos.tablist.ChaosTablist;
import com.chaos.tablist.client.editor.TablistEditorScreen;
import com.chaos.tablist.config.TabConfig;
import com.chaos.tablist.server.TabServer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.scores.DisplaySlot;

/**
 * Graba las capturas y los frames de los GIF de demostración (solo en desarrollo: ./gradlew runShowcase, necesita
 * run/saves/selftest). Deja run/screenshots/sc_still_*.png y sc_&lt;clip&gt;_NNNN.png; tools/make_showcase.py
 * monta con ellos la carpeta screenshots/ de la raíz.
 */
public final class Showcase {

	private record Step(int tick, Consumer<Minecraft> action) {}

	private static final List<Step> STEPS = new ArrayList<>();
	private static int cursor;
	private static int ticks = -1;
	private static int stepIndex;

	// Grabación en curso.
	private static String clip;
	private static int clipStart;
	private static int clipEvery;
	private static int clipFrames;
	private static int clipFrame;

	private Showcase() {}

	public static boolean enabled() {
		return System.getProperty("chaostablist.showcase") != null;
	}

	public static void register() {
		script();
		ClientTickEvents.END_CLIENT_TICK.register(Showcase::tick);
	}

	// ---------------------------------------------------------------------------------------------
	// Guion

	private static void script() {
		at(20, Showcase::setup);
		// El Tab abriéndose y con sus animaciones.
		wait(40, mc -> {
			TabRenderer.testFakes = 12;
			mc.options.keyPlayerList.setDown(true);
		});
		record("tab", 70, 1);
		still("tab");
		wait(4, mc -> {
			mc.options.keyPlayerList.setDown(false);
			TabRenderer.testFakes = 0;
		});

		// El editor.
		wait(10, mc -> serverPlayer(mc, TabServer::open));
		wait(30, mc -> editor(mc, e -> e.setFakePlayersForTest(7)));
		still("editor_general");

		// Editar una línea del header ahí mismo y escribir.
		wait(4, mc -> editor(mc, e -> {
			e.showTab(1);
			e.inlineEditForTest(3);
		}));
		wait(6, mc -> {});
		typing("edit_line", "<gradient:#FFD700:#FF5555>Welcome, </gradient><white>{player}</white><gray>!", 2);
		still("editor_header");

		// La lista de efectos (con sus ejemplos animados).
		wait(4, mc -> editor(mc, e -> e.openPopupForTest(2)));
		record("effects", 40, 2);
		still("effects");

		// El editor de un efecto: cambiar colores y velocidad e insertarlo.
		wait(2, mc -> editor(mc, e -> e.openEffectEditorForTest("gradient_anim")));
		record("effect_editor", 70, 2);
		at(cursor - 60, mc -> editor(mc, e -> e.setEffectValueForTest("c1", "#FF3B3B")));
		at(cursor - 48, mc -> editor(mc, e -> e.setEffectValueForTest("c2", "#FFD700")));
		at(cursor - 36, mc -> editor(mc, e -> e.setEffectValueForTest("speed", "2")));
		at(cursor - 24, mc -> editor(mc, e -> e.setEffectValueForTest("speed", "3")));
		still("effect_editor");
		wait(2, mc -> editor(mc, e -> {
			e.typeForTest(" ");
			e.insertEffectForTest();
		}));
		record("effect_inserted", 30, 2);

		// Datos, iconos y ayuda.
		wait(2, mc -> editor(mc, e -> e.openPopupForTest(4)));
		wait(8, mc -> {});
		still("data");
		wait(2, mc -> editor(mc, e -> {
			e.openPopupForTest(0);
			e.showTab(7);
		}));
		wait(8, mc -> {});
		still("icons");
		wait(2, mc -> editor(mc, e -> e.showTab(8)));
		wait(8, mc -> {});
		still("help");

		// Vista previa de un color antes de pulsar OK (y se deshace al cerrar).
		wait(2, mc -> editor(mc, e -> {
			e.showTab(5);
			e.colorPreviewForTest(0x2A6A3A);
		}));
		record("color_preview", 80, 2);
		int[] colors = {0x6A2A8A, 0x1A4A8A, 0x8A2A2A, 0x2A2A2A, 0x8A6A1A};
		for (int i = 0; i < colors.length; i++) {
			int c = colors[i];
			at(cursor - 70 + i * 13, mc -> editor(mc, e -> e.pickPaletteForTest(c)));
		}
		still("color_preview");
		wait(2, mc -> editor(mc, e -> e.closePopupForTest()));

		// Resto de pestañas y la vista vanilla.
		wait(6, mc -> editor(mc, e -> e.showTab(4)));
		wait(8, mc -> {});
		still("ping");
		wait(2, mc -> editor(mc, e -> e.showTab(6)));
		wait(8, mc -> {});
		still("animations");
		wait(2, mc -> editor(mc, e -> {
			e.showTab(2);
			e.setVanillaPreview(true);
		}));
		wait(8, mc -> {});
		still("vanilla_view");

		// Deshacer (Ctrl+Z) todo lo escrito.
		wait(2, mc -> editor(mc, e -> {
			e.setVanillaPreview(false);
			e.showTab(1);
		}));
		record("undo", 40, 2);
		at(cursor - 32, mc -> editor(mc, TablistEditorScreen::undoForTest));
		at(cursor - 16, mc -> editor(mc, TablistEditorScreen::undoForTest));

		wait(10, mc -> {
			ChaosTablist.LOGGER.info("[showcase] terminado");
			mc.stop();
		});
	}

	private static void setup(Minecraft mc) {
		mc.options.pauseOnLostFocus = false;
		mc.options.guiScale().set(2);
		mc.resizeDisplay();
		server(mc, (server, p) -> {
			server.getPlayerList().op(p.getGameProfile());
			p.serverLevel().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
			p.serverLevel().setDayTime(6000);
			p.serverLevel().setWeatherParameters(6000, 0, false, false);
			// Sin el marcador que deja la autoprueba vanilla.
			server.getScoreboard().setDisplayObjective(DisplaySlot.LIST, null);
			TabServer.apply(server, new TabConfig());
		});
	}

	// ---------------------------------------------------------------------------------------------
	// Pasos

	private static void at(int tick, Consumer<Minecraft> action) {
		STEPS.add(new Step(tick, action));
		cursor = Math.max(cursor, tick);
	}

	private static void wait(int delay, Consumer<Minecraft> action) {
		at(cursor + delay, action);
	}

	private static void still(String name) {
		wait(1, mc -> shot(mc, "sc_still_" + name));
	}

	/** Graba {@code frames} frames, uno cada {@code every} ticks. */
	private static void record(String name, int frames, int every) {
		wait(1, mc -> {
			clip = name;
			clipStart = ticks;
			clipEvery = every;
			clipFrames = frames;
			clipFrame = 0;
		});
		cursor += frames * every;
	}

	/** Escribe {@code text} letra a letra (cada {@code every} ticks) mientras graba. */
	private static void typing(String name, String text, int every) {
		int start = cursor + 1;
		record(name, text.length() + 20, every);
		for (int i = 0; i < text.length(); i++) {
			String ch = String.valueOf(text.charAt(i));
			STEPS.add(new Step(start + 10 + i * every, mc -> editor(mc, e -> e.typeForTest(ch))));
		}
		cursor = Math.max(cursor, start + 10 + text.length() * every + 10);
	}

	private static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
			return;
		}
		ticks++;
		if (stepIndex == 0) {
			STEPS.sort((a, b) -> Integer.compare(a.tick(), b.tick()));
		}
		while (stepIndex < STEPS.size() && STEPS.get(stepIndex).tick() <= ticks) {
			STEPS.get(stepIndex++).action().accept(mc);
		}
		if (clip != null && (ticks - clipStart) % clipEvery == 0) {
			shot(mc, String.format("sc_%s_%04d", clip, clipFrame++));
			if (clipFrame >= clipFrames) {
				clip = null;
			}
		}
	}

	// ---------------------------------------------------------------------------------------------

	private static void editor(Minecraft mc, Consumer<TablistEditorScreen> action) {
		if (mc.screen instanceof TablistEditorScreen e) {
			action.accept(e);
		}
	}

	private interface ServerAction {
		void run(MinecraftServer server, ServerPlayer player);
	}

	private static void server(Minecraft mc, ServerAction action) {
		MinecraftServer server = mc.getSingleplayerServer();
		if (server != null && mc.player != null) {
			java.util.UUID id = mc.player.getUUID();
			server.execute(() -> {
				ServerPlayer p = server.getPlayerList().getPlayer(id);
				if (p != null) {
					action.run(server, p);
				}
			});
		}
	}

	private static void serverPlayer(Minecraft mc, Consumer<ServerPlayer> action) {
		server(mc, (s, p) -> action.accept(p));
	}

	private static void shot(Minecraft mc, String name) {
		Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), msg -> {});
	}
}
