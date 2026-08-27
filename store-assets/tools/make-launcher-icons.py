#!/usr/bin/env python3
"""Rasterise the adaptive launcher icon into the pre-API-26 mipmap fallbacks.

Draws the same geometry as drawable/ic_launcher_foreground.xml on the
launcher_paper background, crops to the 72x72 the launcher actually shows of a
108x108 adaptive canvas, and writes ic_launcher / ic_launcher_round at every
density. Re-run whenever the vector changes.

    python3 make-launcher-icons.py
"""
import os

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "..", "app", "src", "main", "res")

VIEWPORT = 108
VISIBLE = 72
S = 16  # units -> pixels while drawing

PAPER = (250, 245, 234)
STICKY_FACE = (224, 211, 182)
STICKY_HEAD = (217, 204, 173)
STICKY_EDGE = (169, 152, 114)
PENCIL = (111, 106, 94)
INK_BLUE = (46, 90, 168)

ROWS = (47.5, 57.0, 66.5)
RULE_X0, RULE_X1 = 46, 69
DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def px(*v):
    return tuple(round(c * S) for c in v)


def rounded(d, box, radius, fill):
    d.rounded_rectangle(px(*box), radius=round(radius * S), fill=fill)


def draw_icon():
    img = Image.new("RGB", (VIEWPORT * S, VIEWPORT * S), PAPER)
    d = ImageDraw.Draw(img)

    rounded(d, (30, 30, 78, 78), 3, STICKY_EDGE)
    rounded(d, (30, 30, 76, 76), 3, STICKY_FACE)
    rounded(d, (30, 30, 76, 44), 3, STICKY_HEAD)
    d.rectangle(px(30, 38, 76, 44), fill=STICKY_FACE)

    for y in ROWS:
        d.line(px(RULE_X0, y, RULE_X1, y), fill=PENCIL, width=round(2.4 * S))

    for y in ROWS[:2]:
        d.line([px(35.2, y), px(37.8, y + 2.6), px(42.4, y - 3.2)],
               fill=INK_BLUE, width=round(2.8 * S), joint="curve")
        for point in ((35.2, y), (42.4, y - 3.2)):
            cap = 1.4
            d.ellipse(px(point[0] - cap, point[1] - cap, point[0] + cap, point[1] + cap),
                      fill=INK_BLUE)

    y = ROWS[2]
    d.ellipse(px(35.8, y - 3, 41.8, y + 3), outline=PENCIL, width=round(2.2 * S))
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


main()
