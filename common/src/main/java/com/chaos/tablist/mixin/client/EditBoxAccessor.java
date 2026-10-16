package com.chaos.tablist.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.components.EditBox;

/** Para seleccionar texto arrastrando el ratón en los campos del editor (vanilla solo deja con Shift). */
@Mixin(EditBox.class)
public interface EditBoxAccessor {

	@Accessor("displayPos")
	int chaostablist$displayPos();
}
