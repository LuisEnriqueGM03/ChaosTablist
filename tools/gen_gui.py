"""
Genera las texturas del editor del Tab (estilo pixel, marrón oscuro con contorno dorado).

    python tools/gen_gui.py

Escribe sprites nine-slice en src/main/resources/assets/chaostablist/textures/gui/sprites/, que el juego
mete solo en el atlas de la interfaz (se usan como "chaostablist:<nombre>").
"""
import json
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/chaostablist/textures/gui/sprites"


def rgb(h, a=255):
	return ((h >> 16) & 255, (h >> 8) & 255, h & 255, a)


INK = rgb(0x0C0704)
GOLD = rgb(0xE8B23A)
GOLD_LIGHT = rgb(0xFFE08A)
GOLD_DARK = rgb(0x8A5A12)
PANEL = rgb(0x2B1A0E)


def save(name, img, border):
	OUT.mkdir(parents=True, exist_ok=True)
	img.save(OUT / f"{name}.png")
	w, h = img.size
	meta = {"gui": {"scaling": {"type": "nine_slice", "width": w, "height": h, "border": border}}}
	(OUT / f"{name}.png.mcmeta").write_text(json.dumps(meta, indent=2), encoding="utf-8")


def rect(img, x0, y0, x1, y1, c):
	for y in range(y0, y1 + 1):
		for x in range(x0, x1 + 1):
			img.putpixel((x, y), c)


def frame(img, i, top_left, bottom_right=None):
	"""Anillo a `i` píxeles del borde; arriba/izquierda y abajo/derecha pueden ir en colores distintos."""
	w, h = img.size
	bottom_right = bottom_right or top_left
	for x in range(i, w - i):
		img.putpixel((x, i), top_left)
		img.putpixel((x, h - 1 - i), bottom_right)
	for y in range(i, h - i):
		img.putpixel((i, y), top_left)
		img.putpixel((w - 1 - i, y), bottom_right)


def cut_corners(img, n=1):
	w, h = img.size
	clear = (0, 0, 0, 0)
	for k in range(n):
		for (x, y) in [(k, 0), (0, k), (w - 1 - k, 0), (w - 1, k), (k, h - 1), (0, h - 1 - k),
				(w - 1 - k, h - 1), (w - 1, h - 1 - k)]:
			img.putpixel((x, y), clear)


def panel():
	img = Image.new("RGBA", (48, 48), PANEL)
	frame(img, 0, INK)
	frame(img, 1, GOLD_DARK)
	frame(img, 2, GOLD_LIGHT, GOLD)
	frame(img, 3, GOLD_DARK)
	frame(img, 4, INK)
	cut_corners(img, 2)
	img.putpixel((1, 1), INK)
	img.putpixel((46, 1), INK)
	img.putpixel((1, 46), INK)
	img.putpixel((46, 46), INK)
	# Remaches dorados en las esquinas.
	for (cx, cy) in [(5, 5), (41, 5), (5, 41), (41, 41)]:
		rect(img, cx, cy, cx + 1, cy + 1, GOLD)
		img.putpixel((cx, cy), GOLD_LIGHT)
		img.putpixel((cx + 1, cy + 1), GOLD_DARK)
	save("panel", img, 8)


def inset(name, fill, light):
	img = Image.new("RGBA", (16, 16), rgb(fill))
	frame(img, 0, INK, rgb(light))
	frame(img, 1, rgb(0x120B06), rgb(fill))
	save(name, img, 2)


def header():
	"""Barra del título: dorado con relieve."""
	img = Image.new("RGBA", (16, 14), GOLD)
	frame(img, 0, INK)
	for x in range(1, 15):
		img.putpixel((x, 1), GOLD_LIGHT)
		img.putpixel((x, 12), GOLD_DARK)
	for y in range(1, 13):
		img.putpixel((1, y), GOLD_LIGHT)
		img.putpixel((14, y), GOLD_DARK)
	cut_corners(img)
	save("header", img, 3)


def button(name, border, fill, top, bottom):
	img = Image.new("RGBA", (20, 20), rgb(fill))
	frame(img, 0, INK)
	frame(img, 1, rgb(border))
	for x in range(2, 18):
		img.putpixel((x, 2), rgb(top))
		img.putpixel((x, 17), rgb(bottom))
	cut_corners(img)
	save(name, img, 3)


def main():
	panel()
	inset("inset", 0x1A0F07, 0x4A3018)
	inset("cell", 0x33200F, 0x4A3018)
	header()
	button("button", 0x8A5A12, 0x4A2E16, 0x6A4422, 0x301C0C)
	button("button_hover", 0xE8B23A, 0x5E3B1C, 0x84582C, 0x3A2410)
	button("button_disabled", 0x4A3418, 0x2A1A0C, 0x342010, 0x1E1208)
	button("button_selected", 0xFFE08A, 0xE8B23A, 0xFFE08A, 0x8A5A12)
	print("sprites en", OUT)


if __name__ == "__main__":
	main()
