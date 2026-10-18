#!/usr/bin/env python3
"""Turn the raw captures into the captioned screenshots Play shows.

    python3 caption-screenshots.py

Reads store-assets/captures/<language>/ and writes store-assets/screenshots/<language>/
at the same names and sizes: the capture shrunk onto the ink field of the feature
graphic, under a headline in the app's own hand. Each form factor also gets one
drawn card, the sheet that says the app has no internet permission, as its last
screenshot. Re-run it after every reshoot and after any change to a caption.
"""
import glob, os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

TOOLS = os.path.dirname(os.path.abspath(__file__))
STORE = os.path.dirname(TOOLS)
CAPTURES = os.path.join(STORE, "captures")
SCREENSHOTS = os.path.join(STORE, "screenshots")
HAND = os.path.join(os.path.dirname(STORE), "app", "src", "main", "res", "font", "patrick_hand.ttf")

FIELD = (22, 48, 92)
HEADLINE = (250, 245, 234)
SUBLINE = (185, 200, 226)
PAPER = (250, 245, 234)
RULE = (196, 210, 228)
MARGIN = (226, 160, 150)
INK = (40, 38, 34)
INK_BLUE = (46, 90, 168)
STRIKE = (180, 52, 40)

BAND = 0.17        # the headline's share of the height
SHOT = 0.80        # the capture's share of the width and height
CORNER = 0.035     # the capture's corner radius, as a share of the width

CAPTIONS = {
    "en-US": {
        "lists": ("All your to-do lists on one board", "Offline · No account · No ads"),
        "items": ("Tick it, it's crossed off", None),
        "calendar": ("Your week on a paper calendar", None),
        "date": ("One day per list, one reminder per day", None),
        "offline": ("No internet permission", "Your lists never leave your phone."),
        "absent": ("No account", "No ads", "No trackers"),
    },
    "fr-FR": {
        "lists": ("Toutes vos to-do lists sur un seul tableau", "Hors ligne · Sans compte · Sans pub"),
        "items": ("Cochez, c'est rayé", None),
        "calendar": ("Votre semaine sur un calendrier papier", None),
        "date": ("Un jour par liste, un rappel par jour", None),
        "offline": ("Aucune permission Internet", "Vos listes ne quittent jamais votre téléphone."),
        "absent": ("Sans compte", "Sans pub", "Sans traceur"),
    },
}

OFFLINE_CARDS = [
    ("", "phone", "phone-05-offline.png"),
    ("tablet-7", "tablet7", "tablet7-04-offline.png"),
    ("tablet-10", "tablet10", "tablet10-04-offline.png"),
]


def hand(size):
    return ImageFont.truetype(HAND, round(size))


def wrap(d, text, font, width):
    """Greedy for the line count, then balanced, so no word is left alone on a line."""
    words = text.split()
    lines, line = [], ""
    for word in words:
        trial = f"{line} {word}".strip()
        if line and d.textlength(trial, font=font) > width:
            lines.append(line)
            line = word
        else:
            line = trial
    lines.append(line)
    if len(lines) != 2:
        return lines
    splits = [(" ".join(words[:i]), " ".join(words[i:])) for i in range(1, len(words))]
    return list(min(splits, key=lambda s: max(d.textlength(part, font=font) for part in s)))


def headline(img, title, subline):
    w, h = img.size
    d = ImageDraw.Draw(img)
    title_font, sub_font = hand(w * 0.072), hand(w * 0.042)
    title_lines = wrap(d, title, title_font, w * 0.86)
    title_step, sub_step = w * 0.082, w * 0.056
    block = len(title_lines) * title_step + (sub_step + w * 0.012 if subline else 0)
    y = (h * BAND - block) / 2 + w * 0.01
    for line in title_lines:
        d.text((w / 2, y + title_step / 2), line, font=title_font, fill=HEADLINE, anchor="mm")
        y += title_step
    if subline:
        d.text((w / 2, y + w * 0.012 + sub_step / 2), subline, font=sub_font, fill=SUBLINE, anchor="mm")


def card_box(size):
    w, h = size
    cw, ch = round(w * SHOT), round(h * SHOT)
    x, y = (w - cw) // 2, round(h * BAND)
    return x, y, cw, ch


def lay(img, sheet):
    w, _ = img.size
    x, y, cw, ch = card_box(img.size)
    radius = round(w * CORNER)

    shadow = Image.new("L", img.size, 0)
    ImageDraw.Draw(shadow).rounded_rectangle(
        (x, y + w * 0.012, x + cw, y + ch + w * 0.012), radius=radius, fill=120)
    img.paste((0, 0, 0), (0, 0), shadow.filter(ImageFilter.GaussianBlur(w * 0.018)))

    mask = Image.new("L", (cw, ch), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, cw - 1, ch - 1), radius=radius, fill=255)
    img.paste(sheet.resize((cw, ch), Image.LANCZOS), (x, y), mask)


def field(size):
    return Image.new("RGB", size, FIELD)


def captioned(capture, title, subline):
    shot = Image.open(capture).convert("RGB")
    img = field(shot.size)
    lay(img, shot)
    headline(img, title, subline)
    return img


def wifi_struck(d, cx, cy, r):
    stroke = round(r * 0.16)
    for k in (1.0, 0.68, 0.36):
        rr = r * k
        d.arc((cx - rr, cy - rr, cx + rr, cy + rr), start=225, end=315, fill=INK_BLUE, width=stroke)
    dot = r * 0.11
    d.ellipse((cx - dot, cy - dot, cx + dot, cy + dot), fill=INK_BLUE)
    d.line((cx - r * 0.85, cy - r * 1.05, cx + r * 0.85, cy + r * 0.25),
           fill=STRIKE, width=round(stroke * 0.9))


def tick(d, x, y, size):
    d.line((x, y - size * 0.35, x + size * 0.35, y, x + size, y - size * 0.8),
           fill=INK_BLUE, width=round(size * 0.18), joint="curve")


def offline_sheet(size, words, absent):
    """A ruled sheet: the struck wifi, the words, and three ticked absences, all on the rules."""
    w, h = size
    sheet = Image.new("RGB", size, PAPER)
    d = ImageDraw.Draw(sheet)
    rule = w * 0.085
    top = h * 0.12
    y = top
    while y < h - rule / 2:
        d.line((0, y, w, y), fill=RULE, width=max(2, round(w * 0.003)))
        y += rule
    d.line((w * 0.12, 0, w * 0.12, h), fill=MARGIN, width=max(2, round(w * 0.004)))

    wifi_struck(d, w * 0.56, top + rule * 3.6, w * 0.2)

    font = hand(rule * 0.82)
    baseline = top + rule * 6
    for line in wrap(d, words, font, w * 0.74):
        d.text((w * 0.17, baseline - rule * 0.14), line, font=font, fill=INK, anchor="ls")
        baseline += rule
    baseline += rule
    for word in absent:
        tick(d, w * 0.18, baseline - rule * 0.25, rule * 0.5)
        d.text((w * 0.29, baseline - rule * 0.14), word, font=font, fill=INK, anchor="ls")
        baseline += rule
    return sheet


def offline_card(size, title, words, absent):
    img = field(size)
    _, _, cw, ch = card_box(size)
    lay(img, offline_sheet((cw, ch), words, absent))
    headline(img, title, None)
    return img


def main():
    for language, captions in CAPTIONS.items():
        source = os.path.join(CAPTURES, language)
        target = os.path.join(SCREENSHOTS, language)
        captures = sorted(glob.glob(os.path.join(source, "**", "*.png"), recursive=True))
        if not captures:
            raise SystemExit(f"caption-screenshots: no captures in {source}")
        for capture in captures:
            kind = os.path.basename(capture)[:-len(".png")].rsplit("-", 1)[1]
            out = os.path.join(target, os.path.relpath(capture, source))
            os.makedirs(os.path.dirname(out), exist_ok=True)
            captioned(capture, *captions[kind]).save(out)
        title, words = captions["offline"]
        for subdir, prefix, name in OFFLINE_CARDS:
            size = Image.open(glob.glob(os.path.join(source, subdir, f"{prefix}-*.png"))[0]).size
            offline_card(size, title, words, captions["absent"]).save(os.path.join(target, subdir, name))
        print("captioned", language)


main()
