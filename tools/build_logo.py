"""Mod logo: an airframe silhouette and the PJ:AS wordmark.

The mark is usually seen at thumbnail size in the mod list, so it carries one
idea only."""
from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src/main/resources/logo.png"
PREVIEW = ROOT / "docs"

SIZE = 512
BACKGROUND = (250, 250, 247)
INK = (26, 30, 36)
ACCENT = (176, 58, 46)

FONT_DIR = Path("/mnt/skills/examples/canvas-design/canvas-fonts")
FALLBACK_DIR = Path("/usr/share/fonts/truetype/dejavu")


def font(names, size: int) -> ImageFont.FreeTypeFont:
    for name in names:
        for directory in (FONT_DIR, FALLBACK_DIR):
            path = directory / name
            if path.exists():
                return ImageFont.truetype(str(path), size)
    return ImageFont.load_default()


def drone_mark(draw, centre, scale: float, fill) -> None:
    """Delta planform seen from above, nose up, matching the in-game model."""
    shape = [(0.0, -1.0), (0.86, 0.72), (0.30, 0.46), (0.0, 0.86), (-0.30, 0.46), (-0.86, 0.72)]
    points = [(centre[0] + x * scale, centre[1] + y * scale) for x, y in shape]
    draw.polygon(points, fill=fill)


def build() -> None:
    image = Image.new("RGB", (SIZE, SIZE), BACKGROUND)
    draw = ImageDraw.Draw(image)

    drone_mark(draw, (SIZE / 2, 182), 140.0, INK)

    title = font(["BigShoulders-Bold.ttf", "InstrumentSans-Bold.ttf", "DejaVuSans-Bold.ttf"], 150)
    parts = [("PJ", INK), (":", ACCENT), ("AS", INK)]
    gap = 10
    widths = [draw.textlength(text, font=title) for text, _ in parts]
    total = sum(widths) + gap * (len(parts) - 1)

    box = draw.textbbox((0, 0), "PJ:AS", font=title)
    x = SIZE / 2 - total / 2
    y = 418 - (box[3] + box[1]) / 2
    for (text, colour), width in zip(parts, widths):
        draw.text((x, y), text, font=title, fill=colour)
        x += width + gap

    OUT.parent.mkdir(parents=True, exist_ok=True)
    image.save(OUT)
    print(f"  {OUT.relative_to(ROOT)} — {SIZE}x{SIZE}")

    PREVIEW.mkdir(parents=True, exist_ok=True)
    image.save(PREVIEW / "logo.png")
    print(f"  {(PREVIEW / 'logo.png').relative_to(ROOT)}")


if __name__ == "__main__":
    print("Mod logo:")
    build()
