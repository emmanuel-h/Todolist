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

- On Android 12+ the app follows the device's wallpaper palette via `DynamicColors.applyToActivitiesIfAvailable()`; on API 24–31 it uses a brand palette seeded from `#7C3AED` (refined violet).
- No hex value from the stock M3 template palette (`#6750A4` family) exists in `colors.xml`.
- The full `surfaceContainer*` token family (Lowest/Low/Container/High/Highest) is defined in both light and dark themes.
- Depth is tonal: window background = `surfaceContainerLowest`; list item cards = `Widget.Material3.CardView.Filled` on `surfaceContainer` at `0dp` elevation; no drop shadows in the list content area.
- The tutorial overlay retains `Widget.Material3.CardView.Elevated` at 6–8dp intentionally (it floats above a scrim).

---

## Navigation

```
TodoListsActivity  ("My Lists")
        │
        │  tap a list row
        ▼
TodoListActivity  ("<list name>")
        │
        │  back / up button
        ▼
TodoListsActivity  ("My Lists")
```

---

## Screen 1 — My Lists

### Empty state

```
┌─────────────────────────────────┐
│  My Lists                       │
├─────────────────────────────────┤
│                                 │
│        ┌───────────────┐        │
│        │ [checklist    │        │
│        │  illustration]│        │
│        └───────────────┘        │
│                                 │
│          No lists yet           │
│    Tap + to create your         │
│          first list.            │
│                                 │
│                             (+) │  ← FAB
└─────────────────────────────────┘
```

### Normal state

```
┌─────────────────────────────────┐
│  My Lists                       │
├─────────────────────────────────┤
│  Groceries                   →  │
│  Work tasks                  →  │
│  Weekend                     →  │
│                                 │
│                             (+) │
└─────────────────────────────────┘
```

### Behaviors

**Create a list**
- Tap the FAB → dialog: text field (hint "List name") + Cancel / Create buttons
- Create is a no-op if the field is blank
- On confirm: new list inserted at the top

**Open a list**
- Tap anywhere on a list row → navigates to Screen 2 for that list

**Edit a list name** — _implemented · [#4](https://github.com/emmanuel-h/Todolist/issues/4)_
- Tap the pencil icon (dimmed, left of the list name) → dialog pre-filled with the current name + icon-only Cancel / Save
- On confirm with a non-blank name: row label updates immediately; items and position are unaffected
- On confirm with a blank name or on cancel: dialog dismisses, original name is unchanged

**Set a target date on a list** — _implemented · [#9](https://github.com/emmanuel-h/Todolist/issues/9)_
- At creation: tap the calendar icon in the inline add row → `DatePickerDialog` → selected date attaches to the new list; selecting a target date clears any selected due date
- On an existing list: open the rename dialog → the calendar toggle button (`btnToggleTargetDate`) is checked by default when no date is set; tap the boxed date field (`layoutDateBox`) to open `DatePickerDialog`; the clear button (visible only when a date is set) removes the date without changing the toggle selection; switching the toggle to due-date mode moves the existing date across to the due-date kind
- The date is displayed on a second line of the list row, below the list name, with a calendar icon
- Dates in the past are shown with `colorOnSurfaceVariant` tint (muted); future dates use `colorPrimary` tint
- The year is shown only when the target date falls in a different year from the current year
- Date format uses ICU `getBestDateTimePattern` with skeleton `EEEdMMM` / `EEEdMMMy` for locale-correct output with zero string resources
- The target date is purely informational — it signals "do this ON that specific day" (a milestone, not a deadline); it does not affect list sort order
- `colorError` tint must never be applied to target dates; it is reserved exclusively for overdue due dates
- **Mutual exclusion with due date**: a list may have EITHER a target date OR a due date, never both; enforced in `TodoList.init` and in the create/edit use cases via `require`

**Set a due date on a list** — _implemented · [#8](https://github.com/emmanuel-h/Todolist/issues/8)_
- At creation: tap the alarm icon in the inline add row → `DatePickerDialog` → selected date attaches as due date; selecting a due date clears any selected target date
- On an existing list: open the rename dialog → select the alarm toggle button (`btnToggleDueDate`); tap the boxed date field (`layoutDateBox`) to open `DatePickerDialog`; the clear button removes the due date without changing the toggle selection; switching the toggle to target-date mode moves the existing date across; the two toggle buttons share a single dialog — the kind set is always the currently checked button. Note: the inline add row opens the picker directly on icon tap (different from the dialog, by design)
- A caption line (`textDateKindCaption`) below the toggle row spells out the checked kind — "To do on this day" (📅) or "Finish before this day" (⏰) — reflecting the initial selection when the dialog opens and updating on every kind switch (scoped icon-only exception · [#30](https://github.com/emmanuel-h/Todolist/issues/30)); the toggle buttons themselves stay icon-only
- The due date is displayed on a second line of the list row (alarm icon + formatted date), below any target date line
- Three-tier tinting based on the current date via the `Clock` abstraction: FUTURE → `colorPrimary`, TODAY → `colorWarning`, OVERDUE → `colorError`
- `colorWarning` is a custom theme attribute (`#B45309` light / `#F59E0B` dark), declared in `attrs.xml`
- The year is shown only when the due date falls in a different year from the current year
- The due date means "finish BEFORE/BY that day" (a hard deadline); overdue status is signalled by tint only; it does not affect list sort order
- A 12dp arrow-to-limit glyph (`ic_tab_right`, view id `iconDueDateLimit`) is displayed between the alarm icon and the date text on every due-date row, tinted with the same three-tier color as the rest of the line; it must never appear on target-date rows

**Delete a list**
- Tap the delete icon on a row → the row morphs in place into an error-tinted confirm strip
  — no dialog; the list name stays visible in place and the cancel ✕ / confirm ✓ buttons
  slide in from the right (24dp travel + fade, ✓ staggered 50ms behind ✕)
- Confirm (✓) → list and all its items are permanently removed (no undo)
- Cancel (✕) or a tap on the strip background reverts the row; arming a different row's
  delete disarms the first

**Reorder lists** — _implemented · [#6](https://github.com/emmanuel-h/Todolist/issues/6)_
- Long-press-and-drag a row to reorder
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
│ ←  Groceries                    │
├─────────────────────────────────┤
│                                 │
│        ┌───────────────┐        │
│        │ [checklist    │        │
│        │  illustration]│        │
│        └───────────────┘        │
│                                 │
│          No items yet           │
│    Type below to add your       │
│          first item.            │
│                                 │
├─────────────────────────────────┤
│  Add an item…            [send] │  ← pinned at bottom
└─────────────────────────────────┘
```

### Normal state (mix of active + completed)

```
┌─────────────────────────────────────┐
│ ←  Groceries                        │
├─────────────────────────────────────┤
│  Milk                  [✓] [✎] [🗑] │
│  Bread                 [✓] [✎] [🗑] │
│  Eggs                  [✓] [✎] [🗑] │
│  ─── Completed (2) ─────────────── │  ← divider, visible only when both
│  ~~Butter~~            [↩] [✎] [🗑] │    sections are non-empty
│  ~~Coffee~~            [↩] [✎] [🗑] │  ← 50% alpha + strikethrough
│                                     │
├─────────────────────────────────────┤
│  Add an item…                [send] │
└─────────────────────────────────────┘
```

### Item row

Each row has three icon buttons on the right:

| Button | Active item | Completed item |
|--------|-------------|----------------|
| [✓]/[↩] | Mark as done → moves to completed section | Restore → moves to bottom of active section |
| [✎] | Open edit dialog | Open edit dialog |
| [🗑] | Delete immediately (no confirmation) | Delete immediately (no confirmation) |

There is no checkbox. The strikethrough + 50% alpha is the sole visual indicator of completion.

### Behaviors

**Add an item**
- Type in the inline bar, tap send (or press Enter/Done on keyboard)
- No-op if blank; bar clears on success
- New item appended at the bottom of the active section

**Complete an item** — _behavior change needed · [#1](https://github.com/emmanuel-h/Todolist/issues/1) · [#2](https://github.com/emmanuel-h/Todolist/issues/2)_
- Tap [✓] or double-tap anywhere on the row → item moves immediately to the completed section
- Completed items are ordered by completion time (earliest first, most recently completed last)
- State persists in Room

**Uncomplete an item** — _behavior change needed · [#1](https://github.com/emmanuel-h/Todolist/issues/1)_
- Tap [↩] or double-tap anywhere on the row → item moves to the bottom of the active section
- No memory of original position

**Edit an item title** — _implemented · [#3](https://github.com/emmanuel-h/Todolist/issues/3)_
- Tap [✎] → dialog pre-filled with the current title + Cancel / Save
- On confirm: row label updates immediately; completion state and position are preserved

**Delete an item** — _implemented · [#2](https://github.com/emmanuel-h/Todolist/issues/2)_
- Tap [🗑] → item is permanently removed immediately
- No confirmation dialog, no undo

**Reorder active items** — _implemented · [#5](https://github.com/emmanuel-h/Todolist/issues/5)_
- Long-press-and-drag an active item row to reorder within the active section
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

- All data lives in SQLite via Room
- Deleting a list cascades: items are deleted before the list record
- `TodoItem.id` and `TodoList.id` are UUIDs generated at creation; never editable, never
  supplied by the UI
- Insertion order is the default sort; explicit reorder positions are stored as integers
  and persisted per list (or globally for the lists screen)
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

- Each notification's title is the list name; tapping it deep-links into that list's screen
  with the lists screen beneath it in the back stack (`TaskStackBuilder` with the manifest parent).
- Notifications are posted on a dedicated channel ("Reminders"), tagged by list id so two lists
  never overwrite each other's notification.
- The daily 08:00 check runs as a WorkManager unique `PeriodicWorkRequest`
  (`daily_notification_check`, `ExistingPeriodicWorkPolicy.KEEP`), enqueued on app launch with an
  initial delay to the next local 08:00. WorkManager persists it across reboots and crashes —
  no `BOOT_COMPLETED` receiver is needed.
- "Today" is evaluated in the device's local timezone via `Clock.today()`.
- Lists with no due date and no target date, or whose date does not match the above conditions, produce no notification.
- Opening a list that has since been deleted (e.g. from a stale notification) finishes the screen
  immediately (`TodoListState.NotFound`).

### Must NOT happen
- A notification fired for a list whose date does not qualify.
- The daily check lost permanently after device reboot.

---

## First-launch tutorial — _implemented · [#29](https://github.com/emmanuel-h/Todolist/issues/29)_

On the very first launch of `TodoListsActivity` a full-screen phantom-hand overlay plays a five-scene scripted tour using the real screens and real data operations:

1. Taps the FAB and types "🛒 Groceries" into the inline create row.
2. Hovers the target-date icon while a caption pill anchored just below the inline create row shows "📅 To do on this day" (without opening the picker), then moves to the due-date icon — the caption switches to "⏰ Finish before this day" — taps it, picks tomorrow via the real `DatePickerDialog`, submits (the caption fades out once the date is picked), then shows a mock in-overlay notification banner (🔔 + list name + ⏰ + dM-formatted date, resting below the status-bar inset) previewing the daily 08:00 notification.
3. Opens the list and adds "🍎 Apples" and "🥖 Bread".
4. Completes an item, restores it, drags it back to the top by the handle, then completes both items.
5. Returns to the lists screen, deletes the demo list via the in-row confirm strip, and fades out leaving the app empty.

**Skip control**: a bottom-center floating elevated pill containing 5 progress dots (filled up to the current scene) and a ✕ skip button; on the items screen it floats above the pinned inline add bar (88dp bottom margin). The back gesture also cancels. Both cancel paths delete the demo list and prevent the tutorial from ever showing again automatically.

**Replay**: a dimmed (38% alpha) circular-arrow button (`btnReplayTutorial`) is pinned top-right of the lists screen at all times. Tap calls `TutorialViewModel.replay()`, which transitions `Hidden`/`Dismissed` → `ReadyToStart`; no-op while already `ReadyToStart` or `Active`. Does NOT touch the seen flag — the automatic tutorial still shows exactly once ever. The button hides (`GONE`) while the inline create row is open and reappears when the row closes.

**Crash safety**: the seen flag is persisted the moment the demo starts. The demo list id is persisted until cleanup, so a killed-mid-demo leftover is deleted on next launch by `CleanupAbandonedTutorialUseCase`, which runs on every launch.

**Demo strings** ("🛒 Groceries", "🍎 Apples", "🥖 Bread") are Kotlin literals — no string resources are introduced, preserving the icon-only-UI rule. The caption pill is the one sanctioned exception ([#30](https://github.com/emmanuel-h/Todolist/issues/30)): it displays the two `date_kind_*_caption` string resources shared with the edit-list dialog, with the 📅/⏰ emoji prefixes added as Kotlin literals.

### Must NOT happen
- Tutorial reappearing after the first launch has started it (seen flag is written before the overlay appears).
- The demo list surviving a skip or a mid-demo kill (cleanup runs on next launch).
- Static text introduced anywhere in resources by the tutorial (demo strings are Kotlin-only; the two `date_kind_*_caption` resources shared with the edit-list dialog are the sole sanctioned exception · [#30](https://github.com/emmanuel-h/Todolist/issues/30)).
- `replay()` resetting the seen flag or allowing the automatic first-launch tutorial to fire a second time.
- `iconDueDateLimit` appearing on target-date rows.

---

## Planned — not yet implemented

The following behaviors are agreed but not built. Each issue body carries the authoritative design (mockups + acceptance criteria); this section only records what will change so other sections can be read as "current state".

**Gesture-driven rows** — _planned · [#33](https://github.com/emmanuel-h/Todolist/issues/33) · depends on #32 (now implemented)_
- Rows show only the completion circle + title (+ dates/badges on list rows); the permanent drag/complete/edit/delete buttons disappear.
- Swipe end→start = delete (keeps the two-step confirm strip), swipe start→end = edit, long-press = drag; identical map on both screens. Completed items: swipe-delete only.
- Every gesture action is also exposed as a TalkBack custom action.
- Supersedes the `≡` drag-handle wording in the reorder sections above once implemented.

**Motion & haptics** — _planned · [#34](https://github.com/emmanuel-h/Todolist/issues/34) · depends on #33_
- `MaterialContainerTransform` between the lists screen and a list; animated complete (row slides below the ✓ divider), add, and delete; haptic ticks on complete, drag pickup/drop, and swipe-confirm threshold. Respects reduced-motion settings.

**Edge-to-edge** — _planned · [#35](https://github.com/emmanuel-h/Todolist/issues/35)_
- Transparent system bars, inset-aware padding, content scrolls under the status bar behind a background-derived fade scrim.

---

## Out of scope

The following will not be added:

- User accounts, sync, or cloud backup
- Priority levels or tags
- Rich text in titles
- Sharing lists
