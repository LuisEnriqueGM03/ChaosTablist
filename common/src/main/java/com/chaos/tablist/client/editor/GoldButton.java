package com.chaos.tablist.client.editor;

import com.chaos.tablist.ChaosTablist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Botón con las texturas del mod (tools/gen_gui.py): morado con borde dorado; dorado si está seleccionado. */
public class GoldButton extends Button {

	private static final Identifier NORMAL = ChaosTablist.id("button");
	private static final Identifier HOVER = ChaosTablist.id("button_hover");
	private static final Identifier DISABLED = ChaosTablist.id("button_disabled");
	private static final Identifier SELECTED = ChaosTablist.id("button_selected");

	static final int TEXT = 0xFFFFEBC0;
	static final int TEXT_HOVER = 0xFFFFFFFF;
	static final int TEXT_DISABLED = 0xFF6E5E48;
	static final int TEXT_SELECTED = 0xFF2A1606;

	private boolean selected;

	public GoldButton(int x, int y, int width, int height, Component message, OnPress onPress) {
		super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
	}

	/** Pestaña u opción elegida: se pinta en dorado y no se puede pulsar. */
	public GoldButton selected(boolean selected) {
		this.selected = selected;
		this.active = !selected;
		return this;
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		Identifier sprite = selected ? SELECTED : !active ? DISABLED : isHoveredOrFocused() ? HOVER : NORMAL;
		g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, sprite, getX(), getY(), getWidth(), getHeight());
		int color = selected ? TEXT_SELECTED : !active ? TEXT_DISABLED : isHoveredOrFocused() ? TEXT_HOVER : TEXT;
		Font font = Minecraft.getInstance().font;
		Component message = getMessage();
        int available = Math.max(0, getWidth() - 8);
        if (font.width(message) > available) message = Component.literal(font.plainSubstrByWidth(message.getString(), available));
        int textY = getY() + (getHeight() - 8) / 2 + 1;
		if (selected) {
			g.text(font, message, getX() + (getWidth() - font.width(message)) / 2, textY, color, false);
		} else {
			g.centeredText(font, message, getX() + getWidth() / 2, textY, color);
		}
	}
}
