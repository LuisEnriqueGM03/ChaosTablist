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
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Si el servidor tiene Chaos Tablist, el Tab lo pinta TabRenderer en lugar del vanilla. */
@Mixin(Hud.class)
abstract class GuiMixin {

	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "extractTabList", at = @At("HEAD"), cancellable = true)
	private void chaostablist$renderTabList(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
		if (TabRenderer.renderHud(minecraft, graphics)) {
			ci.cancel();
		}
	}
}
