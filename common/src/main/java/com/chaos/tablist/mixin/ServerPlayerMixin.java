package com.chaos.tablist.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.chaos.tablist.server.TabTicker;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** El nombre del Tab que reciben los clientes vanilla es el que monta TabTicker. */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {

	@Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
	private void chaostablist$displayName(CallbackInfoReturnable<Component> cir) {
		Component name = TabTicker.displayName(((ServerPlayer) (Object) this).getUUID());
		if (name != null) {
			cir.setReturnValue(name);
		}
	}
}
