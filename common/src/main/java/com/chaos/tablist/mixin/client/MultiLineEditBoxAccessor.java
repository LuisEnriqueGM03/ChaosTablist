package com.chaos.tablist.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;

/** Para insertar etiquetas en el cursor de los cuadros de varias líneas del editor. */
@Mixin(MultiLineEditBox.class)
public interface MultiLineEditBoxAccessor {

	@Accessor("textField")
	MultilineTextField chaostablist$textField();
}
