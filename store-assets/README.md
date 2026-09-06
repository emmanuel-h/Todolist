# Store assets

Everything the Play Console listing needs, and the tooling that regenerates it.
All of it was redrawn on 2026-09-06 for the row controls, the date slot and the
per-list colours; the 2026-08-27 set shows the page before those, and anything
older in the history shows the retired Material 3 build.

```
listing/
  ic_launcher_play_store.png 512×512 high-res icon
  feature-graphic.png        1024×500
  make-feature-graphic.py    draws it
  full-description-*.txt     en-US, fr-FR
  short-description-*.txt    en-US, fr-FR (80 characters is the Play ceiling)
screenshots/
  phone-0*.png               1080×1920
  tablet-7/                  1200×1920
  tablet-10/                 1600×2560
tools/
  make-demo-database.py      writes the Room database the screenshots show
  capture-screenshots.py     drives the app and takes the captures
```

## The high-res icon

`listing/ic_launcher_play_store.png` is the 108 dp launcher canvas rendered 1:1 onto
512 px with the background full-bleed, and Play applies its own mask to it. It is
written by `tools/make-launcher-icons.py` alongside the legacy mipmaps, from the same
geometry as the vector — it used to be redrawn by hand, which is a copy that can go
stale against the drawable and quietly did. Re-run the tool whenever the vector
changes. See `docs/app-icons.md`.

A second copy sat at `store-assets/ic_launcher_play_store.png` until 2026-09-06,
outside the one path the tool writes. It was exactly the failure the paragraph above
describes — still the retired sticky-note-and-tick mark long after the icon had
become the checklist — so it was deleted rather than regenerated. The generated file
under `listing/` is the only one.

## The feature graphic

```bash
cd listing && python3 make-feature-graphic.py
```

It reads `app/src/main/res/font/patrick_hand.ttf` — the hand the app writes in —
and hard-codes the `PaperPalette.light` tones. Re-run it when the palette moves.

## The screenshots

Captured on one emulator resized to each form factor. The tablet AVDs on this
machine draw a stray `SecondaryHomeHandle` bar across the top of every capture,
so the phone AVD is resized instead — the app only ever sees the window it is
given.

```bash
export ANDROID_HOME=~/Android/Sdk
D=emulator-5554

python3 tools/make-demo-database.py tools/todo_database
python3 tools/make-demo-database.py tools/todo_database_big --big

adb -s $D shell wm size 1080x1920 && adb -s $D shell wm density 420
python3 tools/capture-screenshots.py $D out/phone phone \
    lists items date lists-dark items-dark

adb -s $D shell wm size 1200x1920 && adb -s $D shell wm density 320
SEED_DB=todo_database_big python3 tools/capture-screenshots.py $D out/tablet7 tablet7 \
    lists items lists-dark

adb -s $D shell wm size 1600x2560 && adb -s $D shell wm density 320
SEED_DB=todo_database_big python3 tools/capture-screenshots.py $D out/tablet10 tablet10 \
    lists items lists-dark
```

The script pushes the demo database into the debug build's data directory with
`run-as`. It also puts SystemUI into demo mode — 9:30, full battery, one wi-fi
glyph — and snoozes every standing notification, because the Safety Center shield
sits in the status bar of a fresh emulator and demo mode will not hide it.

Snoozing once is not enough. The app posts its own daily reminder as soon as it
has been seeded and launched, and that glyph lands in the status bar of every
capture after it, so `POST_NOTIFICATIONS` is revoked up front and the snooze is
run again after each launch.

`TODAY` in `make-demo-database.py` is an epoch day and is what makes the amber
"due today" row amber. Move it forward before a fresh capture run, or the dates
in the screenshots will have drifted into the past.

The demo database is written at the schema version the app is on — `IDENTITY` and
`PRAGMA user_version` both have to match `app/schemas/…/<n>.json`, or Room throws
on the first read and the capture run screenshots a crash.

## What the screenshots are chosen to show

| | |
|---|---|
| `01-lists` | the page of lists: both date kinds, the tally rule, a finished list struck through |
| `02-items` | one list open: rings, the add line, the done section |
| `03-calendar` | the paper calendar and the caption that teaches target day from deadline |
| `04`, `05` | the same pages by lamplight |

Six of the demo lists carry a `ListColour` and the rest are plain, so the page
shows the wash without reading as a paint chart. The phone set has three of them
in view; the tablet set, which seeds the longer `--big` database, has all six.
