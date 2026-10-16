package com.chaos.tablist.mixin.client;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.chaos.tablist.client.TabRenderer;
import com.chaos.tablist.client.ClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.world.scores.*;

@Mixin(Gui.class)
abstract class GuiMixin {
    @Shadow @Final private Minecraft minecraft;
    @Redirect(method="render", at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/components/PlayerTabOverlay;render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V"))
    private void chaostablist$vanilla(PlayerTabOverlay overlay, GuiGraphics graphics, int width, Scoreboard board, Objective objective) {
        var config = ClientState.config();
        if(config == null || !config.enabled) overlay.render(graphics,width,board,objective);
    }
    @Inject(method="render",at=@At("TAIL"))
    private void chaostablist$render(GuiGraphics graphics,float delta,CallbackInfo ci) {
        TabRenderer.renderHud(minecraft,graphics);
    }
}
