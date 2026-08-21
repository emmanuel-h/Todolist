# Items Screen in Compose

## What it does
Screen 2 — the items inside one list — is now Jetpack Compose. `TodoListActivity` calls `setContent` and owns no views. The arrangement is unchanged from the View version: same 40dp gutter, same per-row ruling, same six-element row. What changed is underneath, plus one deliberate motion change (see below).

The lists screen is still Views. Both toolkits coexist until [phase 4](compose-migration-plan.md).

## Architecture
- **Layers**: `ui/todolist/` only. Domain, data and `TodoListViewModel` are untouched — the Pitest gate is unchanged at 368/368.
- **The screen is stateless about data.** `TodoListScreen` takes a `TodoListState` and six callbacks. Everything it owns itself lives in `TodoListScreenState`, which the activity holds so the tutorial can reach it.
- **Drag logic is Compose-free.** `DragSession` and the functions around it are plain Kotlin, unit-tested without Robolectric.

## Files
- `ui/todolist/TodoListScreen.kt` — top bar, `LazyColumn`, drag wiring, edge auto-scroll
- `ui/todolist/TodoRow.kt` — one item row, including its inline title editor
- `ui/todolist/InlineAddRow.kt` — the ghost row and its expanded form
- `ui/todolist/SectionDivider.kt` — the two rules and the completed count
- `ui/todolist/DragReorder.kt` — `DragSession`, `settleDrag`, `moved`, `autoScrollDelta`
- `ui/todolist/TodoListScreenState.kt` — add-row state, edit target, staged order, tutorial anchor registry
- `ui/TodoListActivity.kt` — `setContent` plus a `TutorialStage` that drives state instead of view holders
- Tests: `DragReorderTest`, `TodoListScreenStateTest` (plain JUnit), `TodoListScreenTest` (Robolectric + Compose)

**Deleted**: `TodoListAdapter` (313), `TodoItemAnimator` (160), `activity_todo_list.xml`, `item_todo.xml`, `item_todo_inline_add.xml`, and 1,284 lines of UI tests whose assertions already sit in `TodoListViewModel` tests. `item_todo_divider.xml` stays — the lists adapter still inflates it.

## Invariants & contracts
- **Rows are keyed by item id.** That is what makes a completed item *travel* rather than disappear and reappear, and what lets `Modifier.animateItem` do the work `TodoItemAnimator` used to.
- **Drag is addressed within the active section.** `DragSession` is handed only the active rows, so the completed section is unreachable by construction rather than by a bounds check on every move. Completed rows keep the handle's 48dp of space but draw nothing in it, so titles stay on one vertical line.
- **Auto-scroll needs both an edge and somewhere to go.** A row resting at the top of the list is inside the top edge band by definition; without `canScrollUp`/`canScrollDown`, picking up the first row scrolls the list at it forever and carries the row off screen. Found on device, not in tests.
- **`settleDrag` latches its direction.** A drag landing on exactly half a row meets the swap threshold in both directions and would ping-pong forever.
- **The edge-scroll loop only spins while the band is occupied.** Running it for the whole drag keeps the frame clock busy for as long as a finger is down.
- **The staged order is one field.** Both the drag and the tutorial's `MoveActiveItem` publish into `TodoListScreenState.previewOrder`; committing clears it and calls the ViewModel.
- **The tutorial reads bounds, not views.** Anchors register through `Modifier.tutorialAnchor`, which reports screen coordinates from `onGloballyPositioned` and removes itself on dispose. Actions call the ViewModel or set screen state directly — no `performClick`, no `findViewHolderForAdapterPosition`.

## UI
- **Screen(s)**: Screen 2, all states — empty, active-only, completed-only, both sections, add row expanded, row in edit mode, drag in progress.
- **Design decisions**:
  - **Completing an item moves the row instead of replacing it.** The old animator faded the row out of the active section over 200ms, waited 200ms, then grew a new one into the completed section. Because `LazyColumn` keys by id, the same row now springs down past the ghost row and the divider on `PaperMotion.rowPlacement`. Chosen deliberately over a faithful port: it reads like sliding a line down the page, and it costs no code.
  - The top bar is a plain 56dp `Row`, not `TopAppBar`, which defaults to 64dp and would have silently changed the header height.
  - `PaperSurface` draws the paper rather than letting `android:windowBackground` show through. One opaque full-screen rect of overdraw during the interop phases, and `bg_paper.xml` can go in phase 6 without touching this screen.
  - The dragged row lifts onto `paperSheet` with `zIndex`, not elevation — the paper design has no drop shadows.
  - Drag starts after touch slop rather than on touch-down as `ItemTouchHelper.startDrag` did, so a tap that grazes the handle no longer picks the row up.
  - `SectionDivider` does not clear its semantics. The View used `importantForAccessibility="no"`, which hides only the container; `clearAndSetSemantics` would have hidden the count from screen readers too.
