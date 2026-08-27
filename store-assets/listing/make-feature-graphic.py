#!/usr/bin/env python3
"""Draw the 1024x500 Play Store feature graphic.

The composition is the one the listing has always used: the launcher icon and
the wordmark on a flat field, a tilted phone standing off the right edge. What
changed underneath is the app — the icon is the checklist card and the screen
is ruled paper, so the field is what has to carry the contrast.

The field is the pen the app ticks with, not the Material purple the listing
was born in: every colour here is one the pad itself could draw.

    python3 make-feature-graphic.py [ink|shadow|desk|red]
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
SHOT = os.path.join(HERE, "..", "screenshots", "phone-01-lists.png")
SANS = "/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf"
SANS_TEXT = "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"

W, H = 1024, 500
S = 4  # supersampling

FIELDS = {
    # field, the tonal blob behind the phone, the wordmark, the catchphrase.
    # Every field is one of the pad's own darks, so the sheet is never on a
    # colour the app itself could not draw.
    "ink": ((22, 48, 92), (35, 68, 120), (250, 245, 234), (185, 200, 226)),
    "shadow": ((58, 42, 16), (78, 58, 27), (250, 245, 234), (214, 200, 176)),
    "desk": ((20, 18, 16), (34, 30, 25), (250, 245, 234), (201, 191, 172)),
    "red": ((90, 30, 24), (116, 42, 34), (250, 245, 234), (227, 196, 188)),
}

PAPER = (250, 245, 234)
STICKY_FACE = (224, 211, 182)
STICKY_HEAD = (217, 204, 173)
STICKY_EDGE = (169, 152, 114)
PENCIL = (111, 106, 94)
INK_BLUE = (46, 90, 168)
BEZEL = (28, 27, 31)

ICON = (56, 175, 156)          # x, y, size
TILT = -8
PHONE = (682, 0, 270, 500)     # x, y, width, height, before the tilt
CROP = (101, 0, 979, 1560)     # the screenshot without its empty tail
BEZEL_R, SCREEN_R, SCREEN_INSET = 38, 28, 10
PIVOT = (840, 250)


def scale(*v):
    return tuple(round(c * S) for c in v)


def rounded(d, box, radius, fill):
    d.rounded_rectangle(scale(*box), radius=round(radius * S), fill=fill)


def launcher_icon(d, x, y, size):
    """The adaptive icon as a launcher masks it: 72 of its 108 units, squircled."""
    u = size / 72
    p = lambda ux, uy: (x + ux * u, y + uy * u)
    rounded(d, (x, y, x + size, y + size), size * 0.23, PAPER)
    rounded(d, (*p(14, 14), *p(60, 60)), 4 * u, STICKY_EDGE)
    rounded(d, (*p(12, 12), *p(58, 58)), 4 * u, STICKY_FACE)
    rounded(d, (*p(12, 12), *p(58, 26)), 4 * u, STICKY_HEAD)
    d.rectangle(scale(*p(12, 20), *p(58, 26)), fill=STICKY_FACE)

    for uy in (29.5, 39.0, 48.5):
        d.line(scale(*p(28, uy), *p(51, uy)), fill=PENCIL, width=round(2.4 * u * S))
    for uy in (29.5, 39.0):
        d.line([scale(*p(17.2, uy)), scale(*p(19.8, uy + 2.6)), scale(*p(24.4, uy - 3.2))],
               fill=INK_BLUE, width=round(2.8 * u * S), joint="curve")
    cx, cy, rad = *p(20.8, 48.5), 3 * u
    d.ellipse(scale(cx - rad, cy - rad, cx + rad, cy + rad),
              outline=PENCIL, width=round(2.2 * u * S))


def phone_layer(size):
    """The tilted phone, drawn upright on its own layer and rotated into place."""
    layer = Image.new("RGBA", size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    x, y, w, h = PHONE

    d.rounded_rectangle(scale(x + 6, y + 8, x + w + 6, y + h + 8),
                        radius=BEZEL_R * S, fill=(0, 0, 0, 70))
    d.rounded_rectangle(scale(x, y, x + w, y + h),
                        radius=BEZEL_R * S, fill=BEZEL + (255,))

    sx, sy = x + SCREEN_INSET, y + SCREEN_INSET
    sw, sh = w - SCREEN_INSET * 2, h - SCREEN_INSET * 2
    shot = Image.open(SHOT).convert("RGB").crop(CROP).resize(scale(sw, sh), Image.LANCZOS)
    mask = Image.new("L", shot.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        (0, 0, shot.width - 1, shot.height - 1), radius=SCREEN_R * S, fill=255)
    layer.paste(shot, scale(sx, sy), mask)

    return layer.rotate(TILT, resample=Image.BICUBIC, center=scale(*PIVOT))


def main():
    which = sys.argv[1] if len(sys.argv) > 1 else "ink"
    field, glow, title_ink, sub_ink = FIELDS[which]
    out = os.path.join(HERE, "feature-graphic.png" if len(sys.argv) <= 1
                       else "feature-graphic-%s.png" % which)

    img = Image.new("RGBA", (W * S, H * S), field + (255,))
    d = ImageDraw.Draw(img, "RGBA")
    d.ellipse(scale(650, -210, 1150, 290), fill=glow + (128,))

    launcher_icon(d, *ICON)
    d.text(scale(252, 255), "To do list",
           font=ImageFont.truetype(SANS, 78 * S), fill=title_ink, anchor="ls")
    d.text(scale(255, 313), "Simple lists. Nothing else.",
           font=ImageFont.truetype(SANS_TEXT, 32 * S), fill=sub_ink, anchor="ls")

    img.alpha_composite(phone_layer(img.size))
    img.convert("RGB").resize((W, H), Image.LANCZOS).save(out)
    print("wrote", out)


main()
