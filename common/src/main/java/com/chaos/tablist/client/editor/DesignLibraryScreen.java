package com.chaos.tablist.client.editor;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import com.chaos.tablist.Lang;
import com.chaos.tablist.config.TabConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Saved JSON designs are imported into the draft, and remain undoable until explicitly saved. */
final class DesignLibraryScreen extends Screen {
    private final Screen editor;
    private final TabConfig current;
    private final Consumer<TabConfig> apply;
    private List<Path> files = List.of();
    private int page;
    private Component message = Component.empty();
    DesignLibraryScreen(Screen editor, TabConfig current, Consumer<TabConfig> apply) {
        super(Lang.tr("screen.designs")); this.editor = editor; this.current = current; this.apply = apply;
    }
    @Override protected void init() {
        int x = width / 2 - 150, y = 35;
        for (String preset : List.of("chaos", "minimal", "ocean")) {
            addRenderableWidget(new GoldButton(x, y, 300, 20, Lang.tr("preset." + preset), b -> select(DesignLibrary.preset(preset, current))));
            y += 23;
        }
        try { files = DesignLibrary.files(); }
        catch (Exception e) { message = Lang.tr("msg.design_failed"); }
        int visible = Math.max(1, (height - 190) / 23);
        page = Math.min(page, Math.max(0, (files.size() - 1) / visible));
        for (int i = page * visible; i < Math.min(files.size(), (page + 1) * visible); i++) {
            Path file = files.get(i);
            addRenderableWidget(new GoldButton(x, y + 8, 300, 20, Component.literal(font.plainSubstrByWidth(file.getFileName().toString(), 280)), b -> {
                try { select(DesignLibrary.read(file)); }
                catch (Exception e) { message = Lang.tr("msg.design_failed"); }
            }));
            y += 23;
        }
        addRenderableWidget(new GoldButton(x, height - 66, 35, 20, Component.literal("<"), b -> { page--; rebuildWidgets(); })).active = page > 0;
        addRenderableWidget(new GoldButton(x + 40, height - 66, 220, 20, Lang.tr("button.design_folder"), b -> com.mojang.blaze3d.Blaze3D.openPath(DesignLibrary.directory())));
        addRenderableWidget(new GoldButton(x + 265, height - 66, 35, 20, Component.literal(">"), b -> { page++; rebuildWidgets(); })).active = (page + 1) * visible < files.size();
        addRenderableWidget(new GoldButton(x, height - 42, 300, 20, Lang.tr("button.close"), b -> onClose()));
    }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float delta) {
        extractTransparentBackground(g);
        g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, com.chaos.tablist.ChaosTablist.id("panel"), width / 2 - 160, 4, 320, height - 8);
    }
    private void select(TabConfig config) { apply.accept(config); minecraft.gui.setScreen(editor); }
    @Override public void onClose() { minecraft.gui.setScreen(editor); }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);
        g.centeredText(font, title, width / 2, 14, 0xFFE8B23A);
        if (!message.getString().isEmpty()) g.centeredText(font, message, width / 2, height - 16, 0xFFFF5555);
    }
    @Override public boolean isPauseScreen() { return false; }
}
