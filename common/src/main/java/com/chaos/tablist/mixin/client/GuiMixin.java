package com.chaos.tablist.mixin.client;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.chaos.tablist.client.TabRenderer;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;

/** Si el servidor tiene Chaos Tablist, el Tab lo pinta TabRenderer en lugar del vanilla. */
@Mixin(Gui.class)
abstract class GuiMixin {

	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "renderTabList", at = @At("HEAD"), cancellable = true)
	private void chaostablist$renderTabList(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
		if (TabRenderer.renderHud(minecraft, graphics)) {
			ci.cancel();
		}
	}
}
