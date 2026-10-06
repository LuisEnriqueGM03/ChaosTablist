"""
Monta la carpeta screenshots/ (capturas y GIF de demostración) con lo que graba ./gradlew runShowcase.

    ./gradlew runShowcase
    python tools/make_showcase.py

Lee run/screenshots/sc_still_*.png (capturas) y sc_<clip>_NNNN.png (frames de cada GIF).
"""
import re
import shutil
from collections import defaultdict
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "run/screenshots"
OUT = ROOT / "screenshots"

# Milisegundos por frame de cada GIF (frames grabados cada 1 o 2 ticks de 50 ms).
FRAME_MS = {"tab": 50}
DEFAULT_MS = 100
GIF_WIDTH = 960
# Recorte (izq, arriba, der, abajo en fracciones) para centrar el GIF en lo que importa.
CROP = {"tab": (0.18, 0.0, 0.82, 0.78)}

# Nombres finales (orden de lectura en el README).
NAMES = {
	"tab": "01_tab_animated",
	"edit_line": "02_edit_line_in_place",
	"effects": "03_effects_list",
	"effect_editor": "04_effect_editor",
	"effect_inserted": "05_effect_inserted",
	"color_preview": "06_color_preview",
	"undo": "07_undo",
}


def crop(img, name):
	box = CROP.get(name)
	if not box:
		return img
	w, h = img.size
	return img.crop((int(box[0] * w), int(box[1] * h), int(box[2] * w), int(box[3] * h)))


def fit(img, width):
	if img.width <= width:
		return img
	return img.resize((width, round(img.height * width / img.width)), Image.LANCZOS)


def main():
	if OUT.exists():
		shutil.rmtree(OUT)
	OUT.mkdir()
	clips = defaultdict(list)
	for f in sorted(SRC.glob("sc_*.png")):
		if f.name.startswith("sc_still_"):
			img = Image.open(f).convert("RGB")
			name = f.stem[len("sc_still_"):]
			img.save(OUT / f"{name}.png", optimize=True)
			continue
		m = re.match(r"sc_(.+)_(\d{4})\.png", f.name)
		if m:
			clips[m.group(1)].append(f)
	for name, files in clips.items():
		frames = [fit(crop(Image.open(f).convert("RGB"), name), GIF_WIDTH) for f in files]
		# Una paleta común para todos los frames: menos parpadeo y archivo más pequeño.
		palette = frames[len(frames) // 2].quantize(colors=255, method=Image.Quantize.MEDIANCUT)
		gif = [fr.quantize(palette=palette, dither=Image.Dither.NONE) for fr in frames]
		out = OUT / f"{NAMES.get(name, name)}.gif"
		gif[0].save(out, save_all=True, append_images=gif[1:], duration=FRAME_MS.get(name, DEFAULT_MS), loop=0,
				optimize=True, disposal=1)
		print(f"{out.name}: {len(frames)} frames, {out.stat().st_size // 1024} KB")
	print(len(list(OUT.glob("*.png"))), "capturas")


if __name__ == "__main__":
	main()
