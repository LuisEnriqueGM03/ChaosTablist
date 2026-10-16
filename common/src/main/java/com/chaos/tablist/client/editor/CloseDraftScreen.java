package com.chaos.tablist.client.editor;

import com.chaos.tablist.Lang;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** Closing never silently discards a draft. Escape always returns to the editor. */
final class CloseDraftScreen extends Screen {
    private final Screen editor;
    private final Runnable save, discard;
    CloseDraftScreen(Screen editor, Runnable save, Runnable discard) {
        super(Lang.tr("screen.unsaved")); this.editor = editor; this.save = save; this.discard = discard;
    }
    @Override protected void init() {
        int x = width / 2 - 100, y = height / 2;
        addRenderableWidget(new GoldButton(x, y, 200, 20, Lang.tr("button.save_close"), b -> { minecraft.setScreen(editor); save.run(); }));
        addRenderableWidget(new GoldButton(x, y + 24, 200, 20, Lang.tr("button.discard_close"), b -> discard.run()));
        addRenderableWidget(new GoldButton(x, y + 48, 200, 20, Lang.tr("button.keep_editing"), b -> onClose()));
    }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
        renderTransparentBackground(g);
        g.blitSprite(net.minecraft.client.renderer.RenderType::guiTextured, com.chaos.tablist.ChaosTablist.id("panel"), width / 2 - 120, height / 2 - 50, 240, 135);
    }
    @Override public void onClose() { minecraft.setScreen(editor); }
    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        g.drawCenteredString(font, title, width / 2, height / 2 - 35, 0xFFE8B23A);
    }
    @Override public boolean isPauseScreen() { return false; }
}
