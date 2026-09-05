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
    """The adaptive icon as a launcher masks it: 72 of its 108 units, squircled.

    The writing sits straight on the paper — the sheet it used to be drawn on was
    a square the launcher's mask cut a circle around (#73). Unit coordinates here
    are the vector's own, less the 18-unit crop the mask takes off each edge.
    """
    u = size / 72
    p = lambda ux, uy: (x + ux * u, y + uy * u)
    rounded(d, (x, y, x + size, y + size), size * 0.23, PAPER)

    def capped(points, fill, width):
        d.line([scale(*p(*point)) for point in points],
               fill=fill, width=round(width * u * S), joint="curve")
        cap = width / 2
        for ux, uy in (points[0], points[-1]):
            d.ellipse(scale(*p(ux - cap, uy - cap), *p(ux + cap, uy + cap)), fill=fill)

    for uy in (24.0, 36.0, 48.0):
        capped(((32, uy), (58, uy)), PENCIL, 4.0)
    for elbow, apex in ((23.5, 19.0), (35.0, 30.0)):
        capped(((13, elbow), (16.5, elbow + 3.5), (22.5, apex)), INK_BLUE, 4.5)

    cx, cy = p(17, 48.0)
    outer = (4.5 + 3.5 / 2) * u
    d.ellipse(scale(cx - outer, cy - outer, cx + outer, cy + outer),
              outline=PENCIL, width=round(3.5 * u * S))


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
