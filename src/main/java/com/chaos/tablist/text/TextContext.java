package com.chaos.tablist.text;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;

/** Lo que una plantilla necesita para evaluarse: valores de placeholders, el reloj y para quién se pinta. */
public interface TextContext {

	/** Marca de los valores que son un Component en JSON (el badge de Chaos Ranks, por ejemplo). */
	String JSON_MARK = "\u0001json:";

	/** Valor de un placeholder ("" si no existe). */
	String value(String key);

	/** Segundos (el mismo reloj en servidor y cliente para que las animaciones vayan parejas). */
	double time();

	/** Segundo en que se abrió el Tab (para fadein); 0 si no aplica. */
	default double openedAt() {
		return 0;
	}

	/** true = se monta para un cliente sin el mod (los efectos solo-cliente se ignoran). */
	boolean vanilla();

	/** Plantilla del frame actual de la animación {anim:nombre}, o null si no existe. */
	@Nullable
	String animationFrame(String name, double time);

	int pingColor(int ping);

	/** Fuente de los iconos y badges (compacta en el Tab, normal en menús). */
	default ResourceLocation iconFont() {
		return Icons.FONT_COMPACT;
	}

	@Nullable
	HolderLookup.Provider registries();
}
