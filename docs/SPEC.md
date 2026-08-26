# Product Specification — To-do List

## Overview

A personal to-do list Android app. The user manages multiple named lists; each list holds
items that can be checked off. All data persists across restarts via Room/SQLite.

### Design principle — icon-only UI

**All static labels, screen titles, and empty-state copy are forbidden.** Every affordance must be self-explanatory through icons alone. Specifically:
- No toolbar title text on any screen.
- No headline or body text in any empty-state layout — icon only.
- Text-field hints use `"…"` rather than a descriptive label.
- Dynamic content (list names, item titles entered by the user) is exempt.
- **Scoped exception — date-kind wording** (_[#30](https://github.com/emmanuel-h/Todolist/issues/30)_): the 📅 target vs ⏰ due distinction ("to do ON that day" vs "finish BEFORE that day") proved unteachable through icons alone after three wordless iterations. Two locale-translated strings — `date_kind_target_caption` ("To do on this day") and `date_kind_due_caption` ("Finish before this day"), with French in `values-fr/` — may appear in exactly two places: the caption line under the date-kind toggle in the edit-list dialog, and the tutorial's caption pill. No other static words are permitted anywhere, and these strings are deliberately exempt from `IconOnlyUiTest`.

This principle overrides any contradictory suggestion from a UI agent or the wireframes below.

### Theme & surface hierarchy — _implemented · [#32](https://github.com/emmanuel-h/Todolist/issues/32)_

- The palette is a **fixed ink-on-paper set**, not wallpaper-derived, and it is a Kotlin object rather than a resource: `PaperPalette.light` and `PaperPalette.night` in `ui/paper/PaperPalette.kt`. Colours are named for stationery (`paper`, `ink`, `pencil`, `inkBlue`, `inkRed`, `inkAmber`, `stickyNote`, `rule`), and reached through `InkTone` rather than directly — see `ui/paper/InkBudget.kt` for what each tone is allowed to be spent on.
- **The app has a night palette** and follows the system switch. `PaperPalette.night` is the same sheet by lamplight. `res/values-night/themes.xml` dresses the launch window to match, so the splash hands over without a tonal step.
- `res/values/` holds only what a window needs before the first composition: the two `themes.xml` files, `colors.xml` (the flat `@color/paper` ground), `strings.xml`, and `integers.xml`. `android:windowLightNavigationBar` is API 27, so it lives in `values-v27/` and `values-night-v27/` overlays rather than in the base themes.
- There is no `res/layout/`, no `dimens.xml`, and no `attrs.xml`. Dimensions are `PaperDimens`, motion is `PaperMotion`, and the typographic hand is `PaperType`.
- The launcher icon is `@color/paper` plus the `stickyNote*` tones, and flips with `values-night`.

### Paper page — _implemented_

Both screens are one continuous sheet of ruled loose-leaf paper. Nothing in the content area is a flat tonal plane, and nothing floats above the page except the ＋ chip and the tutorial overlay.

- The page is drawn by `PaperSurface`: solid paper tone + a generated fibre grain, nothing else. `android:windowBackground` is the flat `@color/paper` for the frames before the first composition.
- The gutter is `LocalPaperGutter` — where row content and the ruling both start. **The gutter is bare paper**: no punched-hole column, no margin rule. It is `PaperDimens.gutter` (40dp) on a compact window and `PaperDimens.wideGutter` (100dp, a Seyes margin) from the medium width breakpoint up, because a wider window is a wider sheet rather than a wider text measure.
- The page is never wider than `PaperDimens.pageWidth` (640dp) and is centred in its window. From the expanded width breakpoint (840dp — which a phone in landscape reaches) the composition becomes literal: the window is painted with `palette.desk`, the page column carries its own sheet and drops a shadow onto the desk, and the sticky pad and the replay glyph rest on the desk beside it. `PageFit` is read from `WindowSizeClass` in `PaperTheme` and provided as `LocalPageFit`.
- Pulling either page past its last line bends it instead of stretching it: the whole sheet — ruling, ink, back glyph and all — gives ground at half the speed of the finger up to 32dp, the pulled edge takes a shade in daylight and a lit hairline by lamplight, and letting go lays it flat on `PaperMotion.pageMove`. A downward pull with the keyboard up is left alone so it can reach the keyboard, and a reader who has asked for stillness gets no overscroll at all.
- Ruling is drawn both per page and per row: `RuledPage` rules the sheet, and `Modifier.paperRuling` rules a row that has grown past one pitch, so a hairline always meets the text baseline no matter how tall the row gets. Neither mirrors automatically — a `DrawScope` has no layout direction — so both read `layoutDirection` and place the bare gutter on the row's start edge.
- **Both screens use the same row grammar.** List rows are not cards: they are ruled rows carrying `ring · name · dates · counts`, matching the item rows.
- A fully-completed list is marked by strikethrough and 50% alpha on its name only. The former `colorSecondaryContainer` row fill is gone — a coloured block does not belong on paper.
- The open count is a pencil numeral in the margin (`ui/listmeta/OpenCount.kt`), not a filled pill.
- There is no toolbar. The page of items carries its list's name on its own head rule, which grows in whole pitches like any other row so a long name wraps rather than being clipped. The status bar is transparent and the sheet runs unbroken behind it.
- **There are no background watermark illustrations.** Both screens show a bare page when empty. `IconOnlyUiTest` asserts this: no static text drawn on either page.
  The rule governs **ink, not speech**. Every control still carries a `contentDescription`, including the three text fields that create and rename everything — a field that draws only a ghost `…` is otherwise the one affordance on an empty page a screen reader cannot find at all.
- Dialogs are paper slips (`ui/paper/PaperDialog.kt`): a `paperSheet` surface laid over a veil, ruled like the page under it.
- Grain, ruling and the gutter are fixed palette values — paper texture does not take a device hue. The grain tile is baked once per screen density and held in a concurrent map, so a composition that arrives before `preparePaperSheet()` has finished waits for that bake rather than repeating it.
- The ＋ affordance is a pad of sticky notes (`ui/paper/StickyNotePad.kt`); tapping it peels a sheet and opens the line a new list is written on.

---

## Platform floor

- `minSdk` 24, `targetSdk` 36. The app dates everything with `java.time`, which is API 26,
  so **core library desugaring is required** and is enabled in `app/build.gradle.kts`.
  Without it every Android 7.0/7.1 device dies before the first frame.
- Lint's `NewApi` is **fatal**. That is what caught the above, and it is the only thing
  standing between the declared floor and the code drifting off it again. Anything the
  platform gained after 24 either goes behind a version-qualified resource folder (as
  `android:windowLightNavigationBar` does in `values-v27/`) or behind an SDK check.
- `generateLocaleConfig` is on, so the French translation is reachable from the per-app
  language picker. An icon-only app has no settings screen to offer instead.

---

## Navigation

The app is **one window**. `TodoListsActivity` is the only activity; the two screens are two
entries on a Navigation 3 back stack inside it, so the tapped row can travel into the head rule of
the page it opens and back can peel that page off under the finger.

```
one window — TodoListsActivity
┌──────────────────────────────────────┐
│  back stack                          │
│                                      │
│  ListsRoute            the page      │
│      │                 of lists      │
│      │  tap a list row               │
│      ▼                               │
│  ItemsRoute(listId)    a sheet laid  │
│                        over it       │
│      │  back / edge drag / ←         │
│      ▼                               │
│  ListsRoute                          │
└──────────────────────────────────────┘
```

- Opening a list **pushes** `ItemsRoute`: the row's bounds and its name are shared elements that
  travel and grow into the head rule of the items page, the other rows stay in full ink underneath
  until the travel finishes, and the items fade in beneath the arriving sheet.
- Back **peels** the sheet off: the items page slides off to the trailing edge, uncovering the page
  of lists already in place, with a warm shadow (daylight) or a lit hairline (night) along its
  leading edge. A drag from the screen edge seeks that same movement with the finger; releasing
  past the threshold completes the peel and cancelling springs the sheet flat again.
- `android:enableOnBackInvokedCallback` is on, so the peel is a predictive gesture from
  Android 13 up rather than only on 15.
- Every back owner is a `BackHandler` inside the composition, so they are registered in
  composition order and dispatched newest-first: `NavDisplay`'s own, then the add line's
  (enabled only while the pen is on the paper), then the tutorial's (enabled only while a
  demo is on the paper), which is composed after the page stack and therefore outranks it.
  Nothing is registered on the window during `onCreate` — anything there would be registered
  *before* the composition and could never win.

---

## Screen 1 — My Lists

### Empty state

A bare sheet. No illustration, no words — the icon-only rule governs here, and the pad in
the corner is the only affordance.

```
┌─────────────────────────────────┐
│                                 │
│  ─────────────────────────────  │   ← ruling, nothing on it
│  ─────────────────────────────  │
│  ─────────────────────────────  │
│                                 │
│                          ▤      │   ← sticky pad
└─────────────────────────────────┘
```

### Normal state

```
┌─────────────────────────────────┐
│                            ↺    │   ← replay glyph, top right
│  ○  Groceries          3  ──── │
│  ○  Work tasks   ⏰ 4 Mar  1 ── │
│  ○  Weekend      📅 6 May  ──── │
│  ──── 2 ──────────────────────  │   ← tally rule, done below
│  ○  ~~Holiday~~           ────  │
│                          ▤      │
└─────────────────────────────────┘
```

### Behaviors

**Create a list**
- Tap the sticky pad → a sheet peels off and the add line unfolds under the head rule, hint `…`
- The keyboard's own Done commits the line; there is no send glyph
- Commit is a no-op if the line is blank
- On commit: new list inserted at the top
- Putting the pen down (back, a tap on bare paper, dismissing the keyboard) folds the line away **without discarding what was written** — reopening it finds the words and any day circled still there. Only a commit clears it.

**Open a list**
- Tap anywhere on a list row → navigates to Screen 2 for that list

**Edit a list name** — _implemented · [#4](https://github.com/emmanuel-h/Todolist/issues/4)_
- Swipe a row start→end → the edit sheet opens, pre-filled with the current name and the list's date marks
- Dismissing the sheet **commits** what is on it; there is no confirm row. A blank name is refused and leaves the original unchanged
- The name updates immediately; items and position are unaffected

**Set a target date on a list** — _implemented · [#9](https://github.com/emmanuel-h/Todolist/issues/9)_
- At creation: tap the 📅 mark on the add line's date rule → the paper calendar (`ui/paper/PaperCalendar.kt`) → the day attaches to the list the line commits; circling a target date clears any due date
- On an existing list: open the edit sheet → the 📅 mark is ringed by default when no date is set; tap the date field beside the marks to open the calendar; the clear mark (drawn only when a date is set) removes the date without changing which mark is ringed; ringing ⏰ instead moves the existing date across to the due-date kind
- The date is displayed on a second line of the list row, below the list name, with a calendar icon
- A target date whose day has passed is **struck through** and drawn in `InkTone.Crossed`; one still ahead is plain `InkTone.Margin` pencil. The strike carries the distinction — the two tones alone sit a twentieth of a step apart, which is a difference only the code can see
- The year is shown only when the target date falls in a different year from the current year
- Date format uses ICU `getBestDateTimePattern` with zero string resources, in two hands: the jot drawn in the margin uses the short `dMMM` / `dMMMyy`, and the fuller `EEEdMMM` / `EEEdMMMy` is what the jot is *described* as to a screen reader and what the edit sheet's date field shows
- The locale is read from the configuration rather than from `Locale.getDefault()`, so choosing a language in the per-app picker redraws the dates instead of waiting for the process to restart
- The target date is purely informational — it signals "do this ON that specific day" (a milestone, not a deadline); it does not affect list sort order
- `InkTone.Lost` must never be applied to target dates; red is reserved for a row being torn off and for a day already missed
- **Mutual exclusion with due date**: a list may have EITHER a target date OR a due date, never both; enforced in `TodoList.init` and in the create/edit use cases via `require`

**Set a due date on a list** — _implemented · [#8](https://github.com/emmanuel-h/Todolist/issues/8)_
- At creation: tap the ⏰ mark on the add line's date rule → the paper calendar → the day attaches as a due date; circling a due date clears any target date
- On an existing list: open the edit sheet → ring the ⏰ mark; tap the date field to open the calendar; the clear mark removes the due date without changing which mark is ringed; ringing 📅 instead moves the existing date across. The two marks share one sheet — the kind set is always the ringed one. Note: the add line opens the calendar directly on mark tap, by design
- The two `date_kind_*_caption` strings are the app's only sanctioned copy ([#30](https://github.com/emmanuel-h/Todolist/issues/30)). They are currently spoken only by the tutorial's caption pill — **the edit sheet does not show them.** Either restore a caption rule under the date marks or record here how the 📅/⏰ distinction is taught to a reader who skipped the tour; it must not be left to two glyphs differing by an ink circle
- The due date is displayed on a second line of the list row (alarm icon + formatted date), below any target date line
- Three-tier tinting based on the current date via the `Clock` abstraction: FUTURE → `InkTone.Margin`, TODAY → `InkTone.Today` (amber), OVERDUE → `InkTone.Lost` (red)
- The year is shown only when the due date falls in a different year from the current year
- The due date means "finish BEFORE/BY that day" (a hard deadline); overdue status is signalled by tint only; it does not affect list sort order
- The due date's jot is marked by the ⏰ glyph; the target date's by 📅. Neither appears on the other's row

**Delete a list**
- Swipe a row end→start → the row tears off the page and an undo slip takes its place
- The slip stands for **9 seconds** (`UNDO_SLIP_MILLIS`). Tapping it puts the row back
- The write happens when the slip expires, when another row is torn off, or when the page
  leaves — whichever comes first. Leaving commits rather than forgetting: the tear was the
  decision, and the reader watched the row come off
- The write is made on a scope the composition root owns, not the page's, so backing out of
  a page mid-slip still lands it
- Tearing off a row that is already pending is not a second delete and is ignored
- Confirmed deletes cascade: the list and all its items go

**Reorder lists** — _implemented · [#6](https://github.com/emmanuel-h/Todolist/issues/6)_
- Long-press-and-drag a row to reorder; TalkBack reaches the same move through the
  `Move up` / `Move down` custom actions on the row
- The page hands the repository **the ids of the rows it is showing, in the order it is
  showing them** — never a pair of indices. The page hides a torn-off row for the length of
  its undo slip while the repository still holds it, so index spaces disagree and a drag
  would otherwise move a different list
- A row the page did not name — one finished, or one held behind a slip — keeps the slot it
  had rather than being renumbered around
- Explicit position persists across restarts
- New lists are always inserted at the top

### Must NOT happen
- Navigating into a deleted list
- Creating a list with a blank name
- List data lost on restart

---

## Screen 2 — Todo List

### Empty state

```
┌─────────────────────────────────┐
│ ←  Groceries                    │   ← head rule, the list's own name
│  ─────────────────────────────  │
│     …                           │   ← the add line, hint breathing
│  ─────────────────────────────  │
│                                 │
└─────────────────────────────────┘
```

The add line is a row **between the sections**, not a bar pinned to the bottom of the
window, and it carries no send glyph.

### Normal state (mix of active + completed)

```
┌─────────────────────────────────────┐
│ ←  Groceries                        │
│  ○  Milk                     ────── │
│  ○  Bread                    ────── │
│  ○  Eggs                     ────── │
│     …                        ────── │   ← the add line
│  ──── 2 ──────────────────────────  │   ← tally rule: only when both
│  ●  ~~Coffee~~               ────── │     sections have rows
│  ●  ~~Butter~~               ────── │   ← most recent first
└─────────────────────────────────────┘
```

### Item row

A row carries a completion ring at its **start** and its title. There are no permanent
buttons; every verb is a gesture, and every gesture is also a TalkBack custom action
(`ui/paper/RowVerbs.kt`).

| Gesture | Active item | Completed item |
|---------|-------------|----------------|
| Tap the ring | Marks it done — the tick draws over 440ms, then the row crosses the divider | Restores it to the bottom of the active section |
| Tap the title | Opens an editor in place on the row | Opens an editor in place on the row |
| Swipe end→start | Tears the row off behind a 9-second undo slip | Same |
| Long-press and drag | Reorders within the active section | Not reorderable |

The ring carries `Role.Checkbox` for a screen reader. The pen strike plus a dimmed tone is
the drawn indicator of completion.

Several rows can be mid-tick at once: the pending set is a set, one effect per row, so
ticking a list faster than the stroke lands every tick rather than only the last.

### Behaviors

**Add an item**
- Write on the add line and press the keyboard's Done. There is no send glyph
- No-op if blank; the line clears on success and leaves a fresh caret waiting
- New item appended at the bottom of the active section
- On an empty page the line's hint breathes, across the top of its ink range so it stays
  legible at the bottom of the cycle

**Complete an item** — _behavior change needed · [#1](https://github.com/emmanuel-h/Todolist/issues/1) · [#2](https://github.com/emmanuel-h/Todolist/issues/2)_
- Tap [✓] or double-tap anywhere on the row → item moves immediately to the completed section
- Completed items are ordered by completion time, **most recently completed first** — the
  item that just crossed the divider lands at the top of the done section, where the reader
  is already looking
- State persists in Room

**Uncomplete an item** — _behavior change needed · [#1](https://github.com/emmanuel-h/Todolist/issues/1)_
- Tap [↩] or double-tap anywhere on the row → item moves to the bottom of the active section
- No memory of original position

**Edit an item title** — _implemented · [#3](https://github.com/emmanuel-h/Todolist/issues/3)_
- Tap the title → an editor opens in place on the row, pre-filled
- Losing focus commits; a blank title is refused
- Completion state and position are preserved

**Delete an item** — _implemented · [#2](https://github.com/emmanuel-h/Todolist/issues/2)_
- Swipe end→start → the row tears off and an undo slip stands in its place for 9 seconds
- Same rules as a list: tapping the slip restores it, and the write lands when the slip
  expires, when another row is torn off, or when the page leaves — on a scope that outlives
  the page, so walking back out mid-slip still lands it

**Reorder active items** — _implemented · [#5](https://github.com/emmanuel-h/Todolist/issues/5)_
- Long-press-and-drag an active row to reorder within the active section
- As on the page of lists, the page hands down **the ids it is showing, in order** — never
  indices. Completed rows and rows held behind an undo slip are named by nobody and keep
  the slots they had
- Completed items cannot be manually reordered (always ordered by completion time)
- Explicit position persists across restarts

### Must NOT happen
- A completed item appearing above any active item
- A blank item being created
- Item data lost on restart or back-navigation
- Reordering affecting the completed section
- The completed section header appearing when there are no completed items

---

## Data & persistence

- All data lives in SQLite via Room, schema version 7, `exportSchema = true`, with every migration 1→7 present and no destructive fallback
- The database is opened `JournalMode.TRUNCATE`. Auto Backup copies the file and its sidecars independently, and under WAL a snapshot can catch a commit that lives only in the `-wal` half
- Auto Backup is a whitelist naming the database alone (`backup_rules.xml`, `data_extraction_rules.xml`). That is what keeps `shared_prefs/` behind — a permission the old device already spent must not travel, or the restored one is silently reminder-less, and neither must the demo bookkeeping, so a restored install still gets its tour
- Deleting a list cascades via a real `ForeignKey.CASCADE`, with `PRAGMA foreign_keys = ON` at every open
- `TodoItem.id` and `TodoList.id` are UUIDs generated at creation; never editable, never
  supplied by the UI
- Insertion order is the default sort; explicit reorder positions are stored as integers
  and persisted per list (or globally for the lists screen). Every writer is monotone, and
  gaps are harmless because every consumer reads relative order
- A reorder is addressed **by id, never by index** — see the reorder behaviours above
- Completion timestamp is stored per item to determine ordering within the completed section

---

## Daily notifications — _implemented · [#12](https://github.com/emmanuel-h/Todolist/issues/12)_

Every day at 08:00 the app posts one Android notification per list that qualifies:

| Condition | Notification body |
|-----------|-------------------|
| List has a **due date set to today** | ⏰ followed by the due date in the locale's numeric day/month order (e.g. "⏰ 10/08") |
| List has a **target date set to tomorrow** | 📅 followed by the target date in the locale's numeric day/month order (e.g. "📅 11/08") |

The body contains no words in any language — the emoji mirrors the in-app iconography
(alarm = due date, calendar = target date) and the date is formatted via
`DateFormat.getBestDateTimePattern` with the `dM` skeleton for the device's format locale.

- Each notification's title is the list name; tapping it clears the task and opens the one window
  with `LIST_ID` on its intent, so it lands on that list's page with the page of lists already
  beneath it on the nav back stack.
- Notifications are posted on a dedicated channel ("Reminders"), tagged by list id so two lists
  never overwrite each other's notification.
- The daily 08:00 check runs as a WorkManager unique `PeriodicWorkRequest`
  (`daily_notification_check`, `ExistingPeriodicWorkPolicy.KEEP`), enqueued on app launch with an
  initial delay to the next local 08:00. WorkManager persists it across reboots and crashes —
  no `BOOT_COMPLETED` receiver is needed.
- "Today" is evaluated in the device's local timezone via `Clock.today()`.
- Lists with no due date and no target date, or whose date does not match the above conditions, produce no notification.
- **The permission ask belongs to the first reminder of either kind**, not to due dates alone — both fire, so gating on the alarm left a reader who only circles calendar days silently un-remindable.
- The ask is owed when the reminder is *persisted*, not when a day is circled on a line that has not been committed; that line may never become a list.
- The record of having asked is written when the answer comes back, not when the dialog is raised, so an unanswered dialog does not spend the one ask.
- Writing another reminder while notifications are off opens the system's own notification page for the app. An icon-only app has no settings screen, and this is the only route back from a refusal.
- Opening a list that has since been deleted (e.g. from a stale notification) finishes the screen
  immediately (`TodoListState.NotFound`).

### Must NOT happen
- A notification fired for a list whose date does not qualify.
- The daily check lost permanently after device reboot.
- The one permission ask spent on a question that was never answered, or on a list that was never created.
- A reader left with a reminder date and no route to enable notifications.

---

## First-launch tutorial — _implemented · [#29](https://github.com/emmanuel-h/Todolist/issues/29)_

On the very first launch of `TodoListsActivity` a full-screen phantom-hand overlay plays a five-scene scripted tour using the real screens and real data operations:

1. Taps the FAB and types "🛒 Groceries" into the inline create row.
2. Hovers the target-date icon while a caption pill anchored just below the inline create row shows "📅 To do on this day" (without opening the picker), then moves to the due-date icon — the caption switches to "⏰ Finish before this day" — taps it, picks tomorrow on the real paper calendar, submits (the caption fades out once the date is picked), then shows a mock in-overlay notification banner (🔔 + list name + ⏰ + dM-formatted date, resting below the status-bar inset) previewing the daily 08:00 notification.
3. Opens the list and adds "🍎 Apples" and "🥖 Bread".
4. Completes an item, restores it, drags it back to the top by the handle, then completes both items.
5. Returns to the lists screen and tears the demo list off the page with the same end→start
   drag a reader would use — not a tap, which on that same rectangle is the gesture that
   opens a list — then fades out leaving the app empty.

**Skip control**: a bottom-center floating elevated pill containing 5 progress dots (filled up to the current scene) and a ✕ skip button; on the items screen it floats above the pinned inline add bar (88dp bottom margin). The back gesture also cancels. Both cancel paths delete the demo list and prevent the tutorial from ever showing again automatically.

**Replay**: a dimmed circular-arrow glyph pinned top-right of the lists screen. Tap calls `TutorialViewModel.replay()`, which transitions `Hidden`/`Dismissed` → `ReadyToStart`; no-op while already `ReadyToStart` or `Active`. Does NOT touch the seen flag — the automatic tutorial still shows exactly once ever. The glyph is taken off the page while the add line is open and returns when the line folds away.

**Crash safety**: the seen flag is persisted the moment the demo starts. The demo list id is persisted until cleanup, so a killed-mid-demo leftover is deleted on next launch by `CleanupAbandonedTutorialUseCase`.

`TutorialViewModel.initialize()` is called from every `onCreate` and is **idempotent**: it returns early unless the state is still `Hidden`. Cleanup's first act is to tear off whatever demo list is recorded, which mid-tour is the list the tour is being written on — so without the guard a rotation deleted the demo out from under itself and then declared the tour over, the seen flag having been written at the opening beat.

**Demo strings** ("🛒 Groceries", "🍎 Apples", "🥖 Bread") are Kotlin literals — no string resources are introduced, preserving the icon-only-UI rule. The caption pill is the one sanctioned exception ([#30](https://github.com/emmanuel-h/Todolist/issues/30)): it displays the two `date_kind_*_caption` string resources shared with the edit-list dialog, with the 📅/⏰ emoji prefixes added as Kotlin literals.

### Must NOT happen
- Tutorial reappearing after the first launch has started it (seen flag is written before the overlay appears).
- The demo list surviving a skip or a mid-demo kill (cleanup runs on next launch).
- A rotation mid-tour deleting the demo list or ending the tour.
- Static text introduced anywhere in resources by the tutorial (demo strings are Kotlin-only; the two `date_kind_*_caption` resources are the sole sanctioned exception · [#30](https://github.com/emmanuel-h/Todolist/issues/30)).
- `replay()` resetting the seen flag or allowing the automatic first-launch tutorial to fire a second time.
- The demo miming a gesture that does something else on the same rectangle.
- The demo aiming at an anchor nothing registers — the hand fades off the page and the beat plays on an invisible disc.

---

## Shipped since this section was last written

**Gesture-driven rows** — _implemented · [#33](https://github.com/emmanuel-h/Todolist/issues/33)_
- Rows show only the completion ring and the title (plus dates and counts on list rows).
- Swipe end→start tears the row off behind an undo slip; long-press drags. On the page of
  lists, swipe start→end opens the edit sheet; on the page of items the title is tapped
  instead, which is where that gesture went.
- Every gesture is also a TalkBack custom action (`RowVerbs.spokenVerbs`), including
  `Move up` / `Move down`, which are the screen-reader route to a reorder.

**Motion & haptics** — _implemented · [#34](https://github.com/emmanuel-h/Todolist/issues/34)_
- The tapped row travels into the head rule of the page it opens; back peels that page off.
  `PaperMotion` owns every spec, `PaperHaptics` every buzz. Reduced motion is honoured twice
  over: Compose's own `MotionDurationScale`, and an `animationsEnabled` flag threaded into
  every primitive and re-read whenever the window starts.

**Edge-to-edge** — _implemented · [#35](https://github.com/emmanuel-h/Todolist/issues/35)_
- Transparent system bars, inset-aware padding, and the sheet running unbroken behind them.

---

## Out of scope

The following will not be added:

- User accounts, sync, or cloud backup
- Priority levels or tags
- Rich text in titles
- Sharing lists
