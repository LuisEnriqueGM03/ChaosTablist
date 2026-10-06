package com.chaos.tablist.client;

import java.util.List;
import java.util.UUID;

import com.chaos.tablist.ChaosTablist;
import com.chaos.tablist.client.editor.TablistEditorScreen;
import com.chaos.tablist.config.TabConfig;
import com.chaos.tablist.config.TabStore;
import com.chaos.tablist.server.ServerContext;
import com.chaos.tablist.server.ServerValues;
import com.chaos.tablist.server.TabServer;
import com.chaos.tablist.text.Evaluator;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/**
 * Autoprueba visual, solo en desarrollo (./gradlew runSelftest, necesita run/saves/selftest).
 * Hace capturas del Tab (abriéndose y abierto), de cada pestaña del editor, de los desplegables y de la vista
 * vanilla, prueba el guardado por la red y deja en el log lo que recibiría un cliente sin el mod.
 * Las capturas quedan en run/screenshots/selftest_*.png.
 */
public final class SelfTest {

	private static int ticks = -1;
	private static UUID playerId;

	private SelfTest() {}

	public static boolean enabled() {
		return System.getProperty("chaostablist.selftest") != null;
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(SelfTest::tick);
	}

	private static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
			return;
		}
		mc.options.pauseOnLostFocus = false;
		MinecraftServer server = mc.getSingleplayerServer();
		playerId = mc.player.getUUID();
		ticks++;

		switch (ticks) {
			case 20 -> server.execute(() -> {
				ServerPlayer p = player(server);
				server.getPlayerList().op(p.getGameProfile());
				p.serverLevel().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
				p.serverLevel().setDayTime(6000);
				TabServer.apply(server, new TabConfig());
				if (Boolean.getBoolean("chaostablist.forceVanilla")) {
					// El Tab vanilla no sale con un solo jugador si no hay un marcador en la lista.
					var board = server.getScoreboard();
					var objective = board.getObjective("selftest_tab");
					if (objective == null) {
						objective = board.addObjective("selftest_tab", ObjectiveCriteria.DUMMY, Component.literal("Tab"),
								ObjectiveCriteria.RenderType.INTEGER, true, null);
					}
					board.setDisplayObjective(DisplaySlot.LIST, objective);
				}
			});
			case 40 -> {
				TabRenderer.testFakes = 14;
				mc.options.keyPlayerList.setDown(true);
			}
			case 41 -> shot(mc, "tab_opening");
			case 55 -> shot(mc, "tab");
			case 70 -> shot(mc, "tab_later");
			case 72 -> {
				mc.options.keyPlayerList.setDown(false);
				logVanilla(server);
				mc.player.connection.sendCommand("tablist preview <gradient:#FF5555:#5555FF>Hola {player}</gradient> <icon:crown:#FFD700> <badge:VIP:#7B2CBF:#FFFFFF:star> <pc:{ping}>{ping}ms");
			}
			case 80 -> shot(mc, "chat_preview");
			case 85 -> {
				TabRenderer.testFakes = 0;
				server.execute(() -> TabServer.open(player(server)));
			}
			case 100 -> shot(mc, "editor_general");
			default -> {
				int[] tabs = {1, 2, 3, 4, 5, 6, 7, 8, 9};
				for (int i = 0; i < tabs.length; i++) {
					int base = 105 + i * 12;
					if (ticks == base) {
						editor(mc, e -> e.showTab(tabsIndex(tabs, base)));
					} else if (ticks == base + 8) {
						shot(mc, "editor_tab" + tabsIndex(tabs, base));
					}
				}
				int after = 105 + tabs.length * 12;
				if (ticks == after) {
					editor(mc, e -> {
						e.showTab(1);
						e.openPopupForTest(2);
					});
				} else if (ticks == after + 8) {
					shot(mc, "popup_effect");
				} else if (ticks == after + 10) {
					editor(mc, e -> e.openPopupForTest(4));
				} else if (ticks == after + 18) {
					shot(mc, "popup_data");
				} else if (ticks == after + 20) {
					editor(mc, e -> e.openPopupForTest(1));
				} else if (ticks == after + 28) {
					shot(mc, "popup_color");
				} else if (ticks == after + 30) {
					editor(mc, e -> {
						e.openPopupForTest(0);
						e.setVanillaPreview(true);
					});
				} else if (ticks == after + 38) {
					shot(mc, "editor_vanilla");
				} else if (ticks == after + 40) {
					// Guardado por la red, como el botón "Guardar".
					editor(mc, e -> {
						e.draft().serverName = "Servidor de Prueba";
						e.draft().header.set(1, "<rainbow:1><b>★ {server_name} ★</b></rainbow>");
						e.saveForTest();
					});
				} else if (ticks == after + 50) {
					ChaosTablist.LOGGER.info("[selftest] guardado por red: serverName={} header1={}",
							TabStore.config().serverName, TabStore.config().header.get(1));
					mc.setScreen(null);
					TabRenderer.testFakes = 14;
					mc.options.keyPlayerList.setDown(true);
				} else if (ticks == after + 62) {
					shot(mc, "tab_saved");
				} else if (ticks == after + 65) {
					mc.options.keyPlayerList.setDown(false);
					mc.stop();
				}
			}
		}
	}

	private static int tabsIndex(int[] tabs, int base) {
		return tabs[(base - 105) / 12];
	}

	private static void editor(Minecraft mc, java.util.function.Consumer<TablistEditorScreen> action) {
		if (mc.screen instanceof TablistEditorScreen e) {
			action.accept(e);
		}
	}

	/** Lo que mandaría el servidor a un cliente vanilla (header y nombre), para revisar los respaldos. */
	private static void logVanilla(MinecraftServer server) {
		server.execute(() -> {
			ServerPlayer p = player(server);
			TabConfig cfg = TabStore.config();
			var values = ServerValues.player(p.getUUID());
			ServerContext ctx = new ServerContext(cfg, values, values, server.registryAccess(), ServerContext.now());
			for (String line : List.of(cfg.header.get(1), cfg.header.get(2), cfg.footer.get(1), cfg.footer.get(2))) {
				Component c = Evaluator.toComponent(Evaluator.eval(line, ctx));
				ChaosTablist.LOGGER.info("[selftest] vanilla: {}", c.getString());
			}
			String row = cfg.rowTemplate(values.get("group"), p.getStringUUID()) + cfg.ping.vanillaSuffix;
			ChaosTablist.LOGGER.info("[selftest] vanilla fila: {}", Evaluator.toComponent(Evaluator.eval(row, ctx)).getString());
			ChaosTablist.LOGGER.info("[selftest] valores: {}", values);
		});
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayer(playerId);
	}

	private static void shot(Minecraft mc, String name) {
		Screenshot.grab(mc.gameDirectory, "selftest_" + name + ".png", mc.getMainRenderTarget(),
				msg -> ChaosTablist.LOGGER.info("[selftest] {}", msg.getString()));
	}
}
