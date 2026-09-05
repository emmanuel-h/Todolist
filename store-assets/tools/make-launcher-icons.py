#!/usr/bin/env python3
"""Rasterise the adaptive launcher icon into the pre-API-26 mipmap fallbacks.

Draws the same geometry as drawable/ic_launcher_foreground.xml on the
launcher_paper background, crops to the 72x72 the launcher actually shows of a
108x108 adaptive canvas, and writes ic_launcher / ic_launcher_round at every
density, plus the 512 px high-res icon the Play listing wants. Re-run whenever
the vector changes.

The sheet the writing used to sit on is gone (#73): a sheet drawn inside the
tile has to carry its own silhouette, and the launcher then cuts a circle
around it, which is a square inside a circle. The paper is the whole tile now
and the mask is the silhouette.

    python3 make-launcher-icons.py
"""
import os

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "..", "app", "src", "main", "res")
LISTING = os.path.join(HERE, "..", "listing")

VIEWPORT = 108
VISIBLE = 72
S = 16  # units -> pixels while drawing

PAPER = (250, 245, 234)
PENCIL = (111, 106, 94)
INK_BLUE = (46, 90, 168)

ROWS = (42.0, 54.0, 66.0)
RULE_X0, RULE_X1 = 50, 76
RULE_WIDTH = 4.0
TICK_WIDTH = 4.5
RING_RADIUS = 4.5
RING_WIDTH = 3.5
DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def px(*v):
    return tuple(round(c * S) for c in v)


def capped(d, points, fill, width):
    """A stroke with round caps, which is what the vector's strokeLineCap draws."""
    d.line([px(*point) for point in points], fill=fill, width=round(width * S), joint="curve")
    cap = width / 2
    for x, y in (points[0], points[-1]):
        d.ellipse(px(x - cap, y - cap, x + cap, y + cap), fill=fill)


def draw_icon():
    img = Image.new("RGB", (VIEWPORT * S, VIEWPORT * S), PAPER)
    d = ImageDraw.Draw(img)

    for y in ROWS:
        capped(d, ((RULE_X0, y), (RULE_X1, y)), PENCIL, RULE_WIDTH)

    for elbow, apex in ((41.5, 37.0), (53.0, 48.0)):
        capped(d, ((31, elbow - 0.5), (34.5, elbow + 3.5), (40.5, apex)), INK_BLUE, TICK_WIDTH)

    # PIL strokes inward from the bounding box while SVG centres the stroke on the
    # path, so the box is grown by half a nib to land on the vector's own ring.
    y = ROWS[2]
    outer = RING_RADIUS + RING_WIDTH / 2
    d.ellipse(px(35 - outer, y - outer, 35 + outer, y + outer),
              outline=PENCIL, width=round(RING_WIDTH * S))
    return img


def main():
    icon = draw_icon()
    inset = (VIEWPORT - VISIBLE) // 2
    visible = icon.crop(px(inset, inset, inset + VISIBLE, inset + VISIBLE))

    for density, size in DENSITIES.items():
        square = visible.resize((size, size), Image.LANCZOS)
        square.save(os.path.join(RES, "mipmap-%s" % density, "ic_launcher.webp"),
                    lossless=True)

        mask = Image.new("L", (size * 8, size * 8), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, size * 8 - 1, size * 8 - 1), fill=255)
        round_icon = square.convert("RGBA")
        round_icon.putalpha(mask.resize((size, size), Image.LANCZOS))
        round_icon.save(os.path.join(RES, "mipmap-%s" % density, "ic_launcher_round.webp"),
                        lossless=True)
        print("wrote", density, size)

    # The listing's high-res icon is the full 108-unit canvas at 512 px, unmasked:
    # Play applies its own mask. It used to be redrawn by hand and could go stale
    # against the vector; it is the same drawing as everything else now.
    store = icon.resize((512, 512), Image.LANCZOS)
    store.save(os.path.join(LISTING, "ic_launcher_play_store.png"))
    print("wrote play-store icon 512")


main()
