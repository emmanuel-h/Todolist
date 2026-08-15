# Date Kind Wording

## What it does
Adds a single translated caption line in two places — under the date-kind toggle in the edit-list dialog, and inside a caption pill in the first-launch tutorial — to distinguish the calendar (📅 target: "to do on this day") from the alarm (⏰ due: "finish before this day"). All other daily-use surfaces remain wordless. This is a deliberate, scoped exception to the icon-only UI rule (→ see `icon-only-ui.md`).

## Architecture
- **Layers**: ui only (no domain, data, or ViewModel changes)
- **Key types**: no new types; `TutorialOverlayController` gains `captionPill`/`captionText` fields and `showCaptionPill(text)` / `hideCaptionPill()` suspend helpers
- **Async contract**: caption visibility is driven by the existing suspend scene functions in `TutorialOverlayController`; no new flows

## Files
- `app/src/main/res/values/strings.xml` — added `date_kind_target_caption` ("To do on this day") and `date_kind_due_caption` ("Finish before this day")
- `app/src/main/res/values-fr/strings.xml` — created; French translations only ("À faire ce jour-là", "À terminer avant ce jour")
- `app/src/main/res/layout/dialog_rename_list.xml` — added `textDateKindCaption` `MaterialTextView` (`textAppearanceBodySmall`, `colorOnSurfaceVariant`) between the toggle+date row and the cancel/confirm row
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListsActivity.kt` — `updateRenameDateDisplay()` sets `textDateKindCaption` to `date_kind_target_caption` or `date_kind_due_caption` based on the checked toggle; fires on every caption-update path (dialog open, kind switch, date set/clear)
- `app/src/main/res/layout/overlay_tutorial.xml` — added `tutorialCaptionPill` `MaterialCardView` (16dp corner radius, 148dp bottom margin, `invisible` by default, `importantForAccessibility="no"`) containing `tutorialCaptionText` `MaterialTextView`
- `app/src/main/java/fr/mandarine/todolist/ui/TutorialOverlayController.kt` — `captionPill`/`captionText` wired in `attachToActivity()`/`detachFromActivity()`; `showCaptionPill(text)` / `hideCaptionPill()` suspend helpers; `runScenesOneAndTwo()` extended with beat 1 (hand hovers `btnListInlineDate`, caption shows "📅 " + `date_kind_target_caption`) and beat 2 (hand moves to `btnListInlineDueDate`, caption switches to "⏰ " + `date_kind_due_caption`, then existing `DatePickerDialog` choreography; caption fades out after date picked)
- `app/src/test/java/fr/mandarine/todolist/ui/DateKindCaptionTest.kt` — 8 Robolectric tests: target caption on dialog open with no date, due caption on open with a due-date list, toggle switch both directions, date-carry-across-kind both directions, overlay has `tutorialCaptionPill` invisible by default, overlay has `tutorialCaptionText`

## Invariants & contracts
- `date_kind_target_caption` and `date_kind_due_caption` are the **only** translated string resources added by this feature. The emoji prefixes ("📅 ", "⏰ ") in the tutorial caption are Kotlin literals in `TutorialOverlayController`, not in resources.
- The caption in the rename dialog (`textDateKindCaption`) must be updated on every path that calls `updateRenameDateDisplay()` — dialog open, kind-toggle switch, date set, and date clear. Skipping any path leaves the caption stale.
- `tutorialCaptionPill` must remain `INVISIBLE` (not `GONE`) at rest so it does not shift the layout of the progress pill below it; use `showCaptionPill()`/`hideCaptionPill()` rather than setting visibility directly.
- `tutorialCaptionPill` carries `importantForAccessibility="no"`; do not make it focusable or add a content description — it is a visual teaching aid, not a navigational element.
- This feature is an explicit, bounded exception to the icon-only UI rule. It does **not** open the door to adding wording elsewhere. Any new visible text must be separately justified.
- The `values-fr/strings.xml` file contains only these two strings; do not add other strings to it unless a separate feature requires a French translation.

## UI
- **Screen(s)**: `TodoListsActivity` (rename dialog), tutorial overlay on both activities
- **Layout file(s)**: `res/layout/dialog_rename_list.xml`, `res/layout/overlay_tutorial.xml`
- **Design decisions**: `textAppearanceBodySmall` with `colorOnSurfaceVariant` keeps the caption visually subordinate to the toggle buttons above it — it reads as a hint, not a label. The 148dp bottom margin on `tutorialCaptionPill` ensures it floats above the progress pill without overlap. The caption pill in the tutorial appears during scene 2 only (the date-kind teaching moment) and fades out once the date is picked, so it does not persist into later scenes.
