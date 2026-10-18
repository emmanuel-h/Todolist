# Store assets

Everything the Play Console listing needs, and the tooling that regenerates it.
On 2026-10-18 the listing was rewritten for search: the store title became
"Paper To-Do List – Offline" / "To-do list papier – Hors ligne" (the launcher
name did not change), the descriptions gained the words people search for
(offline, checklist, reminder, due date), and the screenshots gained captions and
a sixth card saying the app has no internet permission.
The screenshots were reshot on 2026-10-18 for the corkboard and the Calendar tab;
the dark shots were dropped then, because the paper has one theme and they had
become copies of the light ones. The 2026-10-03 set was the first in French as well
as English; the rest was redrawn on 2026-09-06 for the row controls,
the date slot and the per-list colours. The 2026-08-27 set shows the page before
those, and anything older in the history shows the retired Material 3 build.

```
listing/
  ic_launcher_play_store.png 512×512 high-res icon
  feature-graphic-*.png      1024×500, one per listing language
  make-feature-graphic.py    draws them
  title-*.txt                en-US, fr-FR (30 characters is the Play ceiling)
  full-description-*.txt     en-US, fr-FR (4000)
  short-description-*.txt    en-US, fr-FR (80)
captures/
  en-US/, fr-FR/             the raw captures, one set per language, named by Play's code
    phone-0*.png             1080×1920
    tablet-7/                1200×1920
    tablet-10/               1600×2560
screenshots/
  en-US/, fr-FR/             what Play shows: the captures captioned, plus the offline card
tools/
  make-demo-database.py      writes the Room database the screenshots show
  capture-screenshots.py     drives the app and takes one language's captures
  reshoot-store.py           runs both languages at every size into captures/, then captions
  caption-screenshots.py     captures/ → screenshots/: headline, field, offline card
  publish-play.py            uploads bundle, notes, listing text and images to Google Play
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

It hard-codes the `PaperPalette.light` tones and draws one graphic per language,
its wordmark the store title over two lines so it clears the phone. Re-run it when
the palette or the title moves, and after every reshoot: the phone in each is that
language's `captures/<language>/phone-01-lists.png`.
`publish-play.py --feature-graphic` uploads each to its own listing language.

## The screenshots

Captured on one emulator resized to each form factor. The tablet AVDs on this
machine draw a stray `SecondaryHomeHandle` bar across the top of every capture,
so the phone AVD is resized instead — the app only ever sees the window it is
given.

```bash
./gradlew installDebug
python3 store-assets/tools/reshoot-store.py emulator-5554
```

`reshoot-store.py` writes the demo databases for each language, resizes the
emulator to each form factor in turn, runs `capture-screenshots.py`, copies the
captures to their final names under `captures/<language>/`, puts the
emulator's size and density back, and runs `caption-screenshots.py`. Look at every image before
publishing: nothing in the run can tell a capture of the page from a capture of
the splash screen.

The French set is the same database with every list and item name translated
(`make-demo-database.py --fr`), and the app switched to French on its own with
`cmd locale set-app-locales` — the system language, and so the status bar, stays
as it is. Dates, the 24-hour time and the Monday-first calendar follow from the
app's locale. A new demo list or item needs its French in `FRENCH`, or the
French run stops on a `KeyError`.

The script pushes the demo database into the debug build's data directory with
`run-as`. It also puts SystemUI into demo mode — 9:30, full battery, one wi-fi
glyph — and snoozes every standing notification, because the Safety Center shield
sits in the status bar of a fresh emulator and demo mode will not hide it.

Snoozing once is not enough. The app posts its own daily reminder as soon as it
has been seeded and launched, and that glyph lands in the status bar of every
capture after it, so `POST_NOTIFICATIONS` is revoked up front and the snooze is
run again after each launch.

A launch waits until the first list is on the page and then a few seconds more,
rather than sleeping a fixed time: a cold start after `wm size` can outlast any
fixed sleep, and the list is in the UI tree while the splash is still fading out
over it, so both halves of the wait are needed.

`TODAY` in `make-demo-database.py` is an epoch day and is what makes the amber
"due today" row amber. Move it forward before a fresh capture run, or the dates
in the screenshots will have drifted into the past.

The demo database is written at the schema version the app is on — `IDENTITY` and
`PRAGMA user_version` both have to match `app/schemas/…/<n>.json`, or Room throws
on the first read and the capture run screenshots a crash.

## Publishing to Google Play

`tools/publish-play.py` talks to the Play Developer Publishing API: it uploads the
bundle and its R8 mapping, releases it on a track with the notes in each language,
and with `--screenshots` empties and refills the phone, 7-inch and 10-inch slots of
every language under `screenshots/`. All of it goes into one edit committed once,
and a failure deletes the edit, so the listing never shows half an update.
`/release-store` runs it; `--dry-run` prints the calls without sending any, and
`--validate-only` has Play check the edit and then throws it away.

```bash
python3 tools/publish-play.py --listing --screenshots --feature-graphic   # the listing alone
python3 tools/publish-play.py --bundle … --mapping … --track production \
    --release-name 2.2.0 --notes-dir notes/ [--screenshots]
```

It needs only `requests`, `PyJWT` and `cryptography`, which the system Python
already has — no Google client library, no fastlane, no Ruby.

### One-time setup

1. In the [Google Cloud console](https://console.cloud.google.com/), pick or create
   a project and enable the **Google Play Android Developer API**.
2. Under *IAM & Admin → Service accounts*, create a service account (no project
   role needed), then *Keys → Add key → JSON*. Save the file as
   `~/.config/todolist/play-service-account.json` and `chmod 600` it. Somewhere
   else works too if `PLAY_SERVICE_ACCOUNT` points at it. Never in the repository —
   `.gitignore` refuses `*service-account*.json` as a backstop.
3. In the [Play Console](https://play.google.com/console/), *Users and permissions →
   Invite new users*, invite the service account's e-mail address, and on the
   app give it *Release to production…*, *Release apps to testing tracks* and
   *Manage store presence*.
4. Check it: `python3 tools/publish-play.py --screenshots --validate-only` signs in,
   builds the edit, has Play validate it, and publishes nothing. A new invitation
   can take a few minutes, occasionally longer, before the API accepts it.

## The captions

The store page is an advert, not the app, so the wordless rule does not reach it:
every screenshot carries a headline in the app's hand on the ink field of the
feature graphic, and the first one a subline with the three absences people search
for. `caption-screenshots.py` holds the words, both languages side by side —
change them there and re-run it; no emulator is needed, because it reads
`captures/`. A capture's caption is chosen by the last word of its name.

| | en-US | fr-FR |
|---|---|---|
| `01-lists` | All your to-do lists on one board | Toutes vos to-do lists sur un seul tableau |
| `02-items` | Tick it, it's crossed off | Cochez, c'est rayé |
| `03-calendar` | Your week on a paper calendar | Votre semaine sur un calendrier papier |
| `04-date` (phone) | One day per list, one reminder per day | Un jour par liste, un rappel par jour |
| `offline` card | No internet permission | Aucune permission Internet |

The offline card is drawn, not captured: a ruled sheet with the wifi glyph struck
through, "your lists never leave your phone" and three ticked absences. It is the
last screenshot of every form factor.

## What the screenshots are chosen to show

| | |
|---|---|
| `01-lists` | the board: both date kinds, the week strip, the highlighter washes |
| `02-items` | one list open: rings, the add line, the done section |
| `03-calendar` | the Calendar tab, a month with its dotted days |
| `04-date` | the date sheet a list's day is circled on |

Six of the demo lists carry a `ListColour` and the rest are plain, so the page
shows the wash without reading as a paint chart. The phone set has three of them
in view; the tablet set, which seeds the longer `--big` database, has all six.
