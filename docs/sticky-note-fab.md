# Sticky-Note Add Button

## What it does
The add-list button on Screen 1 is a pad of sticky notes rather than a round FAB: three muted-yellow sheets stacked at slightly different angles, the top one carrying the ＋. Tapping it peels the top sheet off — it lifts, tilts, drifts up toward the list, and fades — revealing the sheet beneath. Dismissing the inline add row settles a fresh top sheet back onto the pad.

## Architecture
- **Layers**: ui + resources only — no domain, data, or presentation logic changed.
- **Key resources**:
  - `drawable/sticky_note_sheet_top.xml` / `_mid.xml` / `_back.xml` — the three sheet fills, each a 2dp-corner rectangle with a `sticky_note_edge` hairline
  - `drawable/sticky_note_edge.xml` — transparent + hairline stroke, applied as the FAB's `android:foreground` (the FAB's own fill comes from `backgroundTint`)
  - `ShapeAppearance.Paper.Sticky` — 2dp corner, squares off the FAB
- **Views** (`activity_todo_lists.xml`, inside `@id/stickyNotePad`):
  - `stickyNoteBack` (+4°, elevation 1dp) → `stickyNoteMid` (+2°, 2dp) → `fabAddList` (−1°, 4dp) → `stickyNotePeel` (−1°, 6dp, `gone`)

## Files
- `res/values/colors.xml` — `sticky_note`, `sticky_note_mid`, `sticky_note_back`, `sticky_note_edge`
- `res/values/dimens.xml` — `sticky_note_pad_size`, `sticky_note_sheet_size`, `sticky_note_corner`, `sticky_note_peel_travel_x/y`
- `res/values/themes.xml` — `ShapeAppearance.Paper.Sticky`
- `res/layout/activity_todo_lists.xml` — the FAB is wrapped in the `stickyNotePad` `FrameLayout`
- `ui/TodoListsActivity.kt` — `peelStickyNote()`, `settleStickyNote()`, the motion constants, and the delayed IME call
- Deleted: `res/drawable/fab_paper_outline.xml` (the old rounded chip outline)

## Invariants & contracts
- **The peel animates a ghost, never the FAB.** `showInlineAddRow()` sets `fabAddList` to `GONE` synchronously because `TodoListsInlineAddTest` asserts that immediately after `tapFab()`. `stickyNotePeel` is a decorative copy of the top sheet that flies on its own and hides itself at the end, so state and motion are fully decoupled.
- **The IME is delayed by `STICKY_NOTE_PEEL_TOTAL_MS`.** The soft keyboard is a separate window and always draws above the activity, so without the delay it covers the pad ~80 ms into the peel and the animation is never seen. The inline row itself still appears instantly; only `showSoftInput` is postponed.
- **Two-phase motion must be an `AnimatorSet`, not chained `ViewPropertyAnimator`s.** Restarting `view.animate()` from inside its own `withEndAction` silently drops the second phase — the sheet lifts and then freezes. Use `playSequentially(lift, peel)`.
- `stickyNoteMid` / `stickyNoteBack` are hidden and shown alongside the FAB; leaving them up while the FAB is gone shows a headless pad.
- `stickyNotePeel` is an `ImageView`, so it carries a `contentDescription` to satisfy `IconOnlyUiTest.decorativeImagesIn`, plus `importantForAccessibility="no"` so TalkBack skips the decoration.
- The pad box is `sticky_note_pad_size` (72dp) with 56dp sheets centred and `clipChildren="false"`; the rotated sheets need that slack or their corners are clipped. Its 8dp margin plus the 8dp inner slack keeps the visible 16dp screen edge.
- Both `peelStickyNote()` and `settleStickyNote()` no-op under `isReducedMotion()`.
- The FAB stays a `FloatingActionButton` — the tutorial scripts a tap on it and several test classes assert on the type. See `docs/paper-background.md`.

## UI
- **Screen(s)**: `TodoListsActivity` (Screen 1 only — Screen 2 adds items through an inline row, not a FAB)
- **Design decisions**:
  - Muted aged yellow (`#EBDCA4`) rather than office-fluorescent, so the pad reads as a sticky note without fighting the warm paper palette. The sheet edges are warm ochre, not `ink` — a hard black stroke made the note look like a button again.
  - The peel drifts up-**left** (−12dp, −28dp) because new lists insert at the top of the list; the motion points at where the note is about to land.
  - Sheets differ by rotation only, not offset — a 56dp square turned 2° and 4° peeks its corners by roughly 2dp and 4dp, which is the whole stack effect for free.
  - The return is a settle (alpha 0→1, scale 0.9→1, rotation −6°→−1° over 220 ms), so the pad visibly refills rather than popping back.
