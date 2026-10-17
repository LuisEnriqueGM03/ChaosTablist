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

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;
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
    private static long waitingSince;
    private static boolean openingWorld;

	private SelfTest() {}

	public static boolean enabled() {
		return System.getProperty("chaostablist.selftest") != null;
	}


	public static void tick(Minecraft mc) {
  if (mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
  // Open the isolated test world after loader initialization, through the normal title-screen flow.
  if (!openingWorld && mc.getSingleplayerServer() == null && mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen) {
   openingWorld = true;
   if (java.nio.file.Files.exists(mc.gameDirectory.toPath().resolve("saves/selftest/level.dat"))) {
    mc.createWorldOpenFlows().openWorld("selftest", () -> mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen()));
   } else {
    mc.createWorldOpenFlows().createFreshLevel("selftest",
     new net.minecraft.world.level.LevelSettings("ChaosTablist selftest", net.minecraft.world.level.GameType.CREATIVE, new net.minecraft.world.level.LevelSettings.DifficultySettings(net.minecraft.world.Difficulty.PEACEFUL, false, false), true, net.minecraft.world.level.WorldDataConfiguration.DEFAULT),
     new net.minecraft.world.level.levelgen.WorldOptions(83L,false,false),
     registries -> registries.lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions(), new net.minecraft.client.gui.screens.TitleScreen());
   }
  }

		if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
			return;
		}
		mc.options.pauseOnLostFocus = false;
		MinecraftServer server = mc.getSingleplayerServer();
		playerId = mc.player.getUUID();
		ticks++;
        if (ticks == 0) mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);

		switch (ticks) {
			case 20 -> server.execute(() -> {
				ServerPlayer p = player(server);
				server.getPlayerList().op(p.nameAndId());
				p.level().getGameRules().set(GameRules.ADVANCE_TIME, false, server);
				((net.minecraft.world.clock.ServerClockManager)p.level().clockManager()).setTotalTicks(p.level().dimensionType().defaultClock().orElseThrow(), 6000L);
				TabConfig visualConfig = new TabConfig();
                if (!Boolean.getBoolean("chaostablist.forceVanilla")) {
                    visualConfig.footer.add("<head:{player}> <item:minecraft:diamond> <icon:custom/selftest_logo> <alpha:0.4><icon:custom/selftest_logo></alpha>");
                }
                TabServer.apply(server, visualConfig);
                if (!Boolean.getBoolean("chaostablist.forceVanilla")) {
                    try (var png = SelfTest.class.getResourceAsStream("/assets/chaostablist/icon.png")) {
                        if (png == null) throw new IllegalStateException("Missing test PNG");
                        com.chaos.tablist.platform.Services.PLATFORM.send(p, new com.chaos.tablist.net.TabPayloads.IconData("selftest_logo", png.readAllBytes()));
                    } catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
                }
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
			case 55 -> {
                if (!Boolean.getBoolean("chaostablist.forceVanilla") && waitFor(ClientState.player(mc.player.getUUID()).containsKey("x"),
                    "Private player values", "waiting for server")) return;
                if (!Boolean.getBoolean("chaostablist.forceVanilla") && waitFor(CustomIcons.get("selftest_logo") != null, "Custom PNG payload", "waiting for icon texture")) return;
                shot(mc, "tab");
            }
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
			case 100 -> {
                if (waitFor(mc.screen instanceof TablistEditorScreen, "Editor open", mc.screen == null ? "no screen" : mc.screen.getClass().getSimpleName())) return;
                editor(mc, TablistEditorScreen::inputForTest);
                ChaosTablist.LOGGER.info("[selftest] PASS: mouse tab selection, Ctrl+A and Unicode input");
                shot(mc, "editor_general");
            }
			default -> {
				int[] tabs = {1, 2, 3, 4, 5, 6, 7, 8};
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
						e.showTab(1);
						e.openEffectEditorForTest("gradient_anim");
					});
				} else if (ticks == after + 38) {
					shot(mc, "effect_editor");
				} else if (ticks == after + 40) {
					editor(mc, e -> e.openEffectEditorForTest("badge"));
				} else if (ticks == after + 48) {
					shot(mc, "effect_editor_badge");
				} else if (ticks == after + 50) {
					editor(mc, e -> {
						e.closePopupForTest();
						e.showTab(5);
						e.colorPreviewForTest(0x3A7A2A);
					});
				} else if (ticks == after + 58) {
					shot(mc, "color_preview");
				} else if (ticks == after + 60) {
					editor(mc, e -> {
						e.closePopupForTest();
						ChaosTablist.LOGGER.info("[selftest] vista previa deshecha: panelColor={}", e.draft().layout.panelColor);
					});
				} else if (ticks == after + 70) {
					editor(mc, e -> {
						e.openPopupForTest(0);
						e.setVanillaPreview(true);
					});
				} else if (ticks == after + 78) {
					shot(mc, "editor_vanilla");
				} else if (ticks == after + 80) {
					// Guardado por la red, como el botón "Guardar".
					editor(mc, e -> {
						e.draft().serverName = "Servidor de Prueba";
						e.draft().header.set(1, "<rainbow:1><b>★ {server_name} ★</b></rainbow>");
						e.saveForTest();
					});
				} else if (ticks == after + 90) {
					if (!(mc.screen instanceof TablistEditorScreen editor)) throw new IllegalStateException("[selftest] Missing editor");
                    if (waitFor(editor.confirmedForTest(), "Save acknowledgement", editor.saveDebugForTest())) return;
                    if (!TabStore.config().serverName.equals("Servidor de Prueba")) throw new IllegalStateException("[selftest] Server did not apply the saved name");
                    ChaosTablist.LOGGER.info("[selftest] guardado por red: serverName={} header1={}",
							TabStore.config().serverName, TabStore.config().header.get(1));
					mc.setScreen(null);
					TabRenderer.testFakes = 14;
					mc.options.keyPlayerList.setDown(true);
				} else if (ticks == after + 102) {
					shot(mc, "tab_saved");
				} else if (ticks == after + 105) {
					mc.options.keyPlayerList.setDown(false);
					if (Boolean.getBoolean("chaostablist.forceVanilla")) {
                        ChaosTablist.LOGGER.info("[selftest] PASS: vanilla packets, editor and acknowledged network save");
                        mc.stop();
                    } else {
                        TabRenderer.testFakes = 0;
                        server.execute(() -> TabServer.open(player(server)));
                    }
                } else if (ticks == after + 110) {
                    if (waitFor(mc.screen instanceof TablistEditorScreen, "Reopen editor", "waiting for server")) return;
                } else if (ticks == after + 115) {
                    editor(mc, TablistEditorScreen::scrollAdvancedForTest);
                } else if (ticks == after + 120) {
                    shot(mc, "editor_advanced");
                } else if (ticks == after + 122) {
                    editor(mc, TablistEditorScreen::openDesignsForTest);
                } else if (ticks == after + 127) {
                    shot(mc, "design_library");
                } else if (ticks == after + 129) {
                    mc.screen.onClose();
                    editor(mc, e -> e.presetForTest("minimal"));
                } else if (ticks == after + 134) {
                    shot(mc, "preset_minimal");
                    editor(mc, TablistEditorScreen::onClose);
                } else if (ticks == after + 139) {
                    shot(mc, "unsaved_confirmation");
                    mc.screen.onClose();
                } else if (ticks == after + 142) {
                    editor(mc, e -> e.headerForTest("<unknown>Diagnostic test"));
                } else if (ticks == after + 150) {
                    shot(mc, "template_diagnostic");
                    editor(mc, e -> e.headerForTest("<scroll:2:-1>abcdef</scroll>"));
                    server.execute(() -> {
                        TabConfig config = TabStore.config().copy(); config.serverName = "External change";
                        TabServer.apply(server, config);
                    });
                } else if (ticks == after + 160) {
                    if (!(mc.screen instanceof TablistEditorScreen e)) throw new IllegalStateException("[selftest] Missing editor");
                    if (waitFor(e.remoteConflictForTest(), "Server revision broadcast", e.saveDebugForTest())) return;
                    e.saveForTest();
                } else if (ticks == after + 170) {
                    if (!(mc.screen instanceof TablistEditorScreen e)) throw new IllegalStateException("[selftest] Missing editor");
                    if (waitFor(e.conflictForTest(), "Concurrent save rejection", e.saveDebugForTest())) return;
                    shot(mc, "save_conflict");
                } else if (ticks == after + 173) {
                    editor(mc, TablistEditorScreen::reloadForTest);
                } else if (ticks == after + 178) {
                    editor(mc, e -> {
                        if (!e.confirmedForTest()) throw new IllegalStateException("[selftest] Reload did not restore a clean draft");
                    });
                    ChaosTablist.LOGGER.info("[selftest] PASS: presets, close confirmation, diagnostics, network save and conflict recovery");
                    mc.setScreen(null); mc.stop();
                }
			}
		}
	}

    private static boolean waitFor(boolean ready, String step, String detail) {
        if (ready) { waitingSince = 0; return false; }
        long now = net.minecraft.util.Util.getMillis();
        if (waitingSince == 0) waitingSince = now;
        if (now - waitingSince > 15000) throw new IllegalStateException("[selftest] " + step + " timed out: " + detail);
        ticks--;
        return true;
    }

	private static int tabsIndex(int[] tabs, int base) {
		return tabs[(base - 105) / 12];
	}

	private static void editor(Minecraft mc, java.util.function.Consumer<TablistEditorScreen> action) {
		if (mc.screen instanceof TablistEditorScreen e) {
			action.accept(e);
        } else {
            throw new IllegalStateException("[selftest] Expected the editor but it was not open");
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
			String row = cfg.rowTemplate(p.getStringUUID()) + cfg.ping.vanillaSuffix;
			ChaosTablist.LOGGER.info("[selftest] vanilla fila: {}", Evaluator.toComponent(Evaluator.eval(row, ctx)).getString());
			ChaosTablist.LOGGER.info("[selftest] valores: {}", values);
		});
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayer(playerId);
	}

	private static void shot(Minecraft mc, String name) {
		Screenshot.grab(mc.gameDirectory, "selftest_" + name + ".png", mc.getMainRenderTarget(), 1,
				msg -> ChaosTablist.LOGGER.info("[selftest] {}", msg.getString()));
	}
}
