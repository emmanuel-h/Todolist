# Lists Screen in Compose

## What it does
Screen 1 — the page of lists — is now Jetpack Compose. `TodoListsActivity` calls `setContent` and owns no views. `res/layout/` is down to `overlay_tutorial.xml`, so the app renders entirely in Compose apart from the tutorial overlay.

The arrangement is the one that shipped: 72dp rows on ruled paper, the drag handle and pencil on the left, the two count badges and the bin on the right, the target/due line under the name, the replay button top-right and the sticky-note pad bottom-right. Three things changed on purpose (below).

## Architecture
- **Layers**: `ui/todolists/` plus two packages both screens now share, `ui/reorder/` and `ui/tutorial/`. Domain, data and `TodoListsViewModel` are untouched — the Pitest gate is unchanged at 368/368.
- **The screen is stateless about data.** `TodoListsScreen` takes a `TodoListsState` and seven callbacks. Everything it owns itself lives in `TodoListsScreenState`, which the activity holds so the tutorial can reach it.
- **Dates are one kind and one value.** `DateSelection` replaces the two nullable fields the View juggled.

## Files
- `ui/todolists/TodoListsScreen.kt` — the page, drag wiring, both dialogs, the pad and the replay affordance
- `ui/todolists/TodoListRow.kt` — one list row, including its delete-confirm strip
- `ui/todolists/ListInlineAddRow.kt` — the create row
- `ui/todolists/RenameListDialog.kt` — the paper rename card
- `ui/todolists/ListDatePickerDialog.kt` — the Material date picker, dressed in the paper palette
- `ui/todolists/ListDate.kt` — `DateKind`, `DateSelection`, locale formatting, picker-millis conversion
- `ui/todolists/TodoListsScreenState.kt` — create-row state, delete arming, rename target, staged order, drop-in bookkeeping
- `ui/reorder/` — `DragSession`, `settleDrag`, `moved`, `autoScrollDelta`, `orderedBy` (plain Kotlin) plus `Modifier.reorderHandle` and `AutoScrollWhileDragging` (Compose wiring). Moved out of `ui/todolist/` in this phase; both screens use it.
- `ui/tutorial/TutorialAnchors.kt` — the anchor registry and `Modifier.tutorialAnchor`, likewise shared
- `ui/paper/SectionDivider.kt`, `ui/paper/PaperDialog.kt` — promoted into the design system, having gained a second consumer
- `ui/TodoListsActivity.kt` — `setContent` plus a `TutorialStage` that drives state

**Deleted**: `TodoListsAdapter` (347), `TodoListsItemAnimator` (89), `TutorialStageSupport` (32), and eight layouts (869 lines of XML, of which `dialog_add_item`, `dialog_create_list` and `dialog_edit_item` were already dead). 2,634 lines of View-driven tests came out; 1,530 lines of Compose and plain-Kotlin tests went in.

## Invariants & contracts
- **A clickable row swallows its own drag handle.** The handle has to sit outside the row's click target, so the row opens from everything to the right of the handle instead of from the whole row. Every unit test passed with the click on the row; only the drag test caught it.
- **"The first list" means the first row on the page, not the first unfinished one.** By the tutorial's last scene the demo list is finished and has moved below the divider; anchoring to the first *active* row leaves the tutorial pointing at nothing and stalls it. Found on the device, not in tests.
- **The drop-in plays for the row that arrived with a create.** `TodoListsScreenState.dropInFor` compares the ids on screen against the ids it saw last; a fresh id only falls onto the page if a create event is pending. Whether it plays is fixed when the row first composes, because the flag is cleared on the next frame.
- **The staged order is one field.** The drag publishes into `TodoListsScreenState.previewOrder`; committing clears it and calls the ViewModel.
- **Done lists keep the handle but cannot be dragged.** The reorder is addressed within the active section, so a done row has no index to move to — the same construction the items screen uses.
- **The pad is always composed.** Hiding it while the create row is open would cancel the peel mid-flight; it is `taken` instead, which is also what drives the peel.
- **The tutorial reads bounds, not views.** Anchors register through `Modifier.tutorialAnchor`. Actions set screen state or call the ViewModel — no `performClick`, no `getButton(BUTTON_POSITIVE)`.

## UI
- **Screen(s)**: Screen 1, all states — empty, active-only, done-only, both sections, create row open, delete armed, rename dialog, date picker, drag in progress.
- **Design decisions**:
  - **The rename dialog is a sheet, not a card.** Square corners, a hairline rule for an edge, a ruled underline instead of an outlined box, and no elevation. A Material dialog surface always draws a shadow, which the paper design says it has no such thing as. Agreed before the work started.
  - **The date picker is the Material one, in the paper palette.** It replaces `android.app.DatePickerDialog`, whose only way to be driven by the tutorial was `getButton(BUTTON_POSITIVE).performClick()`. Its title is suppressed and its buttons are icons, so the icon-only rule still holds.
  - **A created list still falls onto the page.** `Modifier.animateItem` only fades; the 16dp drop that `TodoListsItemAnimator` gave a new row is kept as a per-row enter transition on `PaperMotion.rowEnter`, so the sheet peeled off the pad and the row that follows read as one movement.
  - **The confirm strip's two rings arrive one after the other**, 50ms apart, via `Modifier.animateEnterExit` — the same stagger the View animated by hand.
  - **The kind toggle carries the date across for free.** Holding one `DateKind` and one date, rather than two nullable dates, removes the code that used to move a value between two fields.
  - The date box and its kind toggle still share a content description, as they did in the View. Two controls answering to "Set target date" is worth fixing the next time these strings are touched.
