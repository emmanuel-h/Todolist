#!/usr/bin/env python3
"""Draw the 1024x500 Play Store feature graphic.

The graphic is the app's own sheet: PaperPalette.light tones, the ruling the
page is drawn on, the launcher's sticky card, and Patrick Hand — the typeface
the app writes in. Re-run whenever the palette or the launcher icon changes.

    python3 make-feature-graphic.py
"""
import os
import random

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
FONT = os.path.join(HERE, "..", "..", "app", "src", "main", "res", "font", "patrick_hand.ttf")
OUT = os.path.join(HERE, "feature-graphic.png")

W, H = 1024, 500
S = 4  # supersampling

PAPER = (250, 245, 234)
RULE = (213, 204, 182)
INK = (43, 36, 32)
INK_DONE = (107, 110, 117)
PENCIL = (111, 106, 94)
INK_BLUE = (46, 90, 168)
INK_AMBER = (143, 93, 18)
STICKY_FACE = (224, 211, 182)
STICKY_HEAD = (217, 204, 173)
STICKY_EDGE = (169, 152, 114)
STICKY_BACK = (195, 180, 143)

GUTTER = 56
HEAD_RULE = 76
PITCH = 62
RULES = [HEAD_RULE + PITCH * i for i in range(1, 7)]
COLUMN = 470
DATE_X = 748
COUNT_X = 928

ROWS = [
    ("Groceries", "alarm", "Aug 29", PENCIL, "4"),
    ("Weekend trip", "calendar", "Sep 2", PENCIL, "3"),
    ("Birthday party", "alarm", "Aug 27", INK_AMBER, "5"),
    ("Reading list", None, None, None, "4"),
]


def font(size):
    return ImageFont.truetype(FONT, size * S)


def scale(*v):
    return tuple(c * S for c in v)


def grain(img):
    random.seed(7)
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            n = random.randint(-3, 3)
            r, g, b = px[x, y]
            px[x, y] = (max(0, min(255, r + n)),
                        max(0, min(255, g + n)),
                        max(0, min(255, b + n)))


def rounded(d, box, radius, fill):
    d.rounded_rectangle(scale(*box), radius=radius * S, fill=fill)


def sticky_card(d, x, y, size):
    """The launcher's card: a back edge, a face, a head strip, a blue tick."""
    r = max(2, size // 18)
    face = size * 23 // 24
    rounded(d, (x + size // 24, y + size // 24, x + size, y + size), r, STICKY_EDGE)
    rounded(d, (x, y, x + face, y + size // 4), r, STICKY_HEAD)
    rounded(d, (x, y + size // 10, x + face, y + face), r, STICKY_FACE)
    p = lambda fx, fy: (x + size * fx, y + size * fy)
    d.line([scale(*p(0.21, 0.59)), scale(*p(0.39, 0.77)), scale(*p(0.78, 0.38))],
           fill=INK_BLUE, width=int(size * 0.075) * S, joint="curve")
    for point in (p(0.21, 0.59), p(0.78, 0.38)):
        cap = size * 0.037
        d.ellipse(scale(point[0] - cap, point[1] - cap, point[0] + cap, point[1] + cap),
                  fill=INK_BLUE)


def sticky_pad(d, x, y, size):
    """The pad the plus sign is peeled from: three sheets and a stroke."""
    rounded(d, (x + 7, y + 7, x + size + 7, y + size + 7), 3, STICKY_BACK)
    rounded(d, (x + 4, y + 4, x + size + 4, y + size + 4), 3, STICKY_EDGE)
    rounded(d, (x, y, x + size, y + size), 3, STICKY_FACE)
    m, arm = x + size // 2, size // 5
    d.line(scale(m - arm, y + size // 2, m + arm, y + size // 2), fill=INK, width=3 * S)
    d.line(scale(m, y + size // 2 - arm, m, y + size // 2 + arm), fill=INK, width=3 * S)


def calendar_mark(d, x, y, size, colour):
    """drawable/ic_event.xml, on a 24-unit grid."""
    u = size / 24
    w = max(1, int(1.8 * u)) * S
    box = (x + 3 * u, y + 4 * u, x + 21 * u, y + 22 * u)
    d.rounded_rectangle(scale(*box), radius=int(2 * u) * S, outline=colour, width=w)
    d.line(scale(x + 3 * u, y + 10 * u, x + 21 * u, y + 10 * u), fill=colour, width=w)
    d.line(scale(x + 8 * u, y + 2 * u, x + 8 * u, y + 6 * u), fill=colour, width=w)
    d.line(scale(x + 16 * u, y + 2 * u, x + 16 * u, y + 6 * u), fill=colour, width=w)


def alarm_mark(d, x, y, size, colour):
    """drawable/ic_alarm.xml, on a 24-unit grid."""
    u = size / 24
    w = max(1, int(1.8 * u)) * S
    d.ellipse(scale(x + 4 * u, y + 5 * u, x + 20 * u, y + 21 * u), outline=colour, width=w)
    d.line(scale(x + 12 * u, y + 9.5 * u, x + 12 * u, y + 13.5 * u), fill=colour, width=w)
    d.line(scale(x + 12 * u, y + 13.5 * u, x + 14.5 * u, y + 15.5 * u), fill=colour, width=w)
    d.line(scale(x + 5 * u, y + 3 * u, x + 2.5 * u, y + 5.5 * u), fill=colour, width=w)
    d.line(scale(x + 19 * u, y + 3 * u, x + 21.5 * u, y + 5.5 * u), fill=colour, width=w)


def main():
    img = Image.new("RGB", (W * S, H * S), PAPER)
    d = ImageDraw.Draw(img)

    d.line(scale(0, HEAD_RULE, W, HEAD_RULE), fill=RULE, width=2 * S)
    d.line(scale(0, HEAD_RULE + 5, W, HEAD_RULE + 5), fill=RULE, width=2 * S)
    for y in RULES:
        d.line(scale(0, y, W, y), fill=RULE, width=2 * S)

    sticky_card(d, GUTTER + 2, 150, 106)

    d.text(scale(GUTTER, RULES[3] - 4), "To do list", font=font(72), fill=INK, anchor="ls")
    d.text(scale(GUTTER + 3, RULES[4] - 6), "Lists on paper.", font=font(30), fill=PENCIL, anchor="ls")

    name_font, date_font, count_font = font(38), font(26), font(26)
    for (name, kind, date, tone, count), y in zip(ROWS, RULES):
        d.text(scale(COLUMN, y - 4), name, font=name_font, fill=INK, anchor="ls")
        if kind:
            mark = alarm_mark if kind == "alarm" else calendar_mark
            mark(d, DATE_X, y - 24, 22, tone)
            d.text(scale(DATE_X + 30, y - 5), date, font=date_font, fill=tone, anchor="ls")
        d.text(scale(COUNT_X, y - 5), count, font=count_font, fill=PENCIL, anchor="ls")

    tally = RULES[4]
    d.text(scale(COLUMN + 232, tally - 5), "2", font=count_font, fill=PENCIL, anchor="ls")

    done = RULES[5]
    d.text(scale(COLUMN, done - 4), "Home office setup", font=name_font, fill=INK_DONE, anchor="ls")
    box = d.textbbox(scale(COLUMN, done - 4), "Home office setup", font=name_font, anchor="ls")
    d.line((box[0] - 6 * S, (done - 14) * S, box[2] + 6 * S, (done - 14) * S),
           fill=INK_DONE, width=2 * S)

    sticky_pad(d, 892, 372, 88)

    img = img.resize((W, H), Image.LANCZOS)
    grain(img)
    img.save(OUT)
    print("wrote", OUT, img.size)


main()
