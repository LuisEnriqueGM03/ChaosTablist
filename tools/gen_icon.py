"""
Genera el icono del mod (assets/chaostablist/icon.png): un Tab en pixel art con borde degradado.

    python tools/gen_icon.py
"""
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/chaostablist/icon.png"
S = 32          # se dibuja a 32x32 y se escala x4 sin suavizar
C1 = (0xB5, 0x7C, 0xF0)
C2 = (0x3F, 0xD8, 0xEA)
BG = (0x16, 0x0C, 0x24)


def lerp(a, b, t):
	return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def main():
	img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
	px = img.load()
	# Panel con esquinas recortadas y borde degradado en diagonal.
	for y in range(S):
		for x in range(S):
			corner = (x in (0, S - 1) and y in (0, 1, S - 2, S - 1)) or (y in (0, S - 1) and x in (0, 1, S - 2, S - 1))
			if corner:
				continue
			edge = x <= 1 or y <= 1 or x >= S - 2 or y >= S - 2
			if edge:
				px[x, y] = lerp(C1, C2, (x + y) / (2 * S - 2)) + (255,)
			else:
				px[x, y] = BG + (255,)
	# Título degradado.
	for x in range(6, 26):
		c = lerp(C1, C2, (x - 6) / 19)
		for y in (5, 6):
			px[x, y] = c + (255,)
	# Filas: cabeza + nombre + barras de ping.
	rows = [(10, 15, (0xFF, 0xC3, 0x00)), (15, 12, (0xFF, 0xFF, 0xFF)), (20, 14, (0x55, 0xFF, 0x55)), (25, 10, (0xC0, 0xC0, 0xC0))]
	heads = [(0xD9, 0x77, 0x3F), (0x7A, 0x5A, 0x3A), (0x3C, 0x8A, 0x2E), (0x4A, 0xA3, 0xFF)]
	for i, (y, length, color) in enumerate(rows):
		for dy in range(3):
			for dx in range(3):
				px[5 + dx, y + dy] = heads[i] + (255,)
			for x in range(10, 10 + length):
				if dy == 1:
					px[x, y + dy] = color + (255,)
		# Barras de ping.
		for b in range(3):
			h = b + 1
			for dy in range(h):
				px[23 + b * 2, y + 2 - dy] = (0x55, 0xFF, 0x55, 255) if i < 3 or b == 0 else (0x55, 0x55, 0x55, 255)
	img.resize((S * 4, S * 4), Image.NEAREST).save(OUT)
	print("icono en", OUT)


if __name__ == "__main__":
	main()
