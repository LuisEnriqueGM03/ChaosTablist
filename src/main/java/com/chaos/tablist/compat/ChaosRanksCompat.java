package com.chaos.tablist.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.jetbrains.annotations.Nullable;

import com.chaos.tablist.ChaosTablist;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Integración opcional con Chaos Ranks (sin depender de él al compilar): rango de cada jugador, su badge
 * (en la fuente compacta del Tab) y su prioridad para ordenar.
 */
public final class ChaosRanksCompat {

	/** Lo que se necesita del rango de un jugador. */
	public record Rank(String id, String label, int priority, Component badge) {}

	private static boolean present;
	private static Method rankOf;
	private static Method build;
	private static Method compact;
	private static Field id;
	private static Field label;
	private static Field priority;

	static {
		if (FabricLoader.getInstance().isModLoaded("chaosranks")) {
			try {
				Class<?> store = Class.forName("com.chaos.ranks.rank.RankStore");
				Class<?> def = Class.forName("com.chaos.ranks.rank.RankDef");
				Class<?> renderer = Class.forName("com.chaos.ranks.badge.BadgeRenderer");
				rankOf = store.getMethod("rankOf", ServerPlayer.class);
				build = renderer.getMethod("build", def);
				compact = renderer.getMethod("compact", Component.class);
				id = def.getField("id");
				label = def.getField("label");
				priority = def.getField("priority");
				present = true;
				ChaosTablist.LOGGER.info("Chaos Ranks detectado: {rank} y el orden por rango están disponibles");
			} catch (ReflectiveOperationException | LinkageError e) {
				ChaosTablist.LOGGER.warn("Chaos Ranks está instalado pero no se pudo enlazar (¿versión distinta?)", e);
			}
		}
	}

	private ChaosRanksCompat() {}

	public static boolean present() {
		return present;
	}

	@Nullable
	public static Rank rankOf(ServerPlayer player) {
		if (!present) {
			return null;
		}
		try {
			Object def = rankOf.invoke(null, player);
			if (def == null) {
				return null;
			}
			Component badge = (Component) compact.invoke(null, build.invoke(null, def));
			return new Rank((String) id.get(def), (String) label.get(def), priority.getInt(def), badge);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}
}
