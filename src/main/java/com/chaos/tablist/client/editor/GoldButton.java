package com.chaos.tablist.client.editor;

import com.chaos.tablist.ChaosTablist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Botón con las texturas del mod (tools/gen_gui.py): morado con borde dorado; dorado si está seleccionado. */
public class GoldButton extends Button {

	private static final ResourceLocation NORMAL = ChaosTablist.id("button");
	private static final ResourceLocation HOVER = ChaosTablist.id("button_hover");
	private static final ResourceLocation DISABLED = ChaosTablist.id("button_disabled");
	private static final ResourceLocation SELECTED = ChaosTablist.id("button_selected");

	static final int TEXT = 0xFFEBC0;
	static final int TEXT_HOVER = 0xFFFFFF;
	static final int TEXT_DISABLED = 0x6E5E48;
	static final int TEXT_SELECTED = 0x2A1606;

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
	protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		ResourceLocation sprite = selected ? SELECTED : !active ? DISABLED : isHoveredOrFocused() ? HOVER : NORMAL;
		g.blitSprite(sprite, getX(), getY(), getWidth(), getHeight());
		int color = selected ? TEXT_SELECTED : !active ? TEXT_DISABLED : isHoveredOrFocused() ? TEXT_HOVER : TEXT;
		Font font = Minecraft.getInstance().font;
		int textY = getY() + (getHeight() - 8) / 2 + 1;
		if (selected) {
			g.drawString(font, getMessage(), getX() + (getWidth() - font.width(getMessage())) / 2, textY, color, false);
		} else {
			g.drawCenteredString(font, getMessage(), getX() + getWidth() / 2, textY, color);
		}
	}
}
