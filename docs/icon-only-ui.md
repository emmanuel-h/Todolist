# Icon-Only UI

## What it does
Strips all decorative and navigational text labels from both screens: the Screen 1 toolbar is removed entirely, dialog action buttons are icon-only (no text), the completed-section divider shows a bare numeric count instead of "Completed (N)", the ghost-row hint collapses to "…", and empty states show only the 120dp illustration with no headline or subheadline.

## Architecture
- **Layers**: ui only (no domain, data, or ViewModel changes)
- **Key types**: no new types; `DividerViewHolder.bind()` updated to emit a plain integer string instead of a format-string result
- **Async contract**: none

## Files
- `app/src/main/res/layout/activity_todo_lists.xml` — removed AppBarLayout + MaterialToolbar entirely; empty-state icon enlarged from 72dp to 120dp; empty-state text views removed
- `app/src/main/res/layout/activity_todo_list.xml` — empty-state icon enlarged from 72dp to 120dp; empty-state text views removed
- `app/src/main/res/layout/dialog_create_list.xml` — replaced text confirm/cancel buttons with icon-only ImageButton views (check + close icons)
- `app/src/main/res/layout/item_todo_list.xml` — in-row delete confirm strip (`layoutDeleteConfirm`) with icon-only confirm (check) and cancel (close) buttons; replaced the earlier `dialog_delete_list.xml` dialog
- `app/src/main/res/layout/dialog_edit_item.xml` — added `android:hint` to TextInputLayout for accessibility labelling (UI agent fix)
- `app/src/main/res/drawable/ic_close.xml` — new close/cancel icon vector drawable
- `app/src/main/res/values/strings.xml` — `completed_section_header` changed from `"Completed (%d)"` to `"%d"`; `add_item_ghost_hint` changed from "Add an item…" to "…"
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListAdapter.kt` — `DividerViewHolder.bind()` sets label to `completedCount.toString()` directly
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListsActivity.kt` — replaced `MaterialAlertDialogBuilder` text-button dialogs with custom icon-only dialog views; removed `setSupportActionBar` call and its `MaterialToolbar` import; added internal test helper methods (`openCreateDialogForTest`, `confirmDialogForTest`, `cancelCurrentDialogForTest`)
- `app/src/test/java/fr/mandarine/todolist/ui/IconOnlyUiTest.kt` — 10 unit tests verifying icon-only UI invariants: divider count-only label, empty-state icon-only (0 text views), toolbar absence, dialog icon buttons
- `app/src/test/java/fr/mandarine/todolist/ui/TodoListAdapterTest.kt` — `should bind divider with correct completed count` asserts `"2"` directly instead of via format string
- `app/src/test/java/fr/mandarine/todolist/ui/TodoListsActivityTest.kt` — dialog interactions rewritten to use internal test helper methods instead of `android.R.id.button1/button2` Espresso selectors

## Invariants & contracts
- **Scoped exception**: `date_kind_target_caption` and `date_kind_due_caption` are translated string resources that appear as a visible caption in the rename dialog and as a tutorial pill caption. This is the only authorised deviation from the no-visible-text rule; it does not permit adding text elsewhere → see `date-kind-wording.md`.
- The Screen 1 toolbar is gone — do not reintroduce `AppBarLayout`, `MaterialToolbar`, or `setSupportActionBar` in `TodoListsActivity`.
- The list name shown on Screen 2 (`TodoListActivity`) is user data, not a UI label; it must remain visible.
- `DividerViewHolder.bind()` must use `completedCount.toString()` — never reintroduce the `"Completed (%d)"` format string.
- Dialog confirm/cancel actions are `ImageButton` views, not `MaterialButton` text buttons; do not replace them with text-bearing buttons.
- Empty states on both screens are icon-only (120dp illustration, no text views); do not add headline or subheadline text.
- Ghost row hint is `"…"` — do not restore "Add an item…" or any other text.
- Delete confirmation is the in-row confirm strip in `item_todo_list.xml` (`layoutDeleteConfirm`), not a dialog; `MaterialAlertDialogBuilder` with default text buttons must not be used for list-level dialogs.
- Internal test helpers (`openCreateDialogForTest`, `confirmDialogForTest`, `cancelCurrentDialogForTest`) exist on `TodoListsActivity` solely to allow unit tests to drive dialogs without Espresso button-id selectors; do not remove them.

## UI
- **Screen(s)**: `TodoListsActivity`, `TodoListActivity`
- **Layout file(s)**: `res/layout/activity_todo_lists.xml`, `res/layout/activity_todo_list.xml`, `res/layout/dialog_create_list.xml`, `res/layout/item_todo_list.xml`, `res/layout/dialog_edit_item.xml`
- **Design decisions**: Icon-only dialogs use a custom inflated view rather than the standard `MaterialAlertDialogBuilder` positive/negative button API, since that API only supports text labels. The 120dp empty-state icon size was chosen to remain visually prominent without text context. Removing the Screen 1 toolbar eliminates the "My Lists" string entirely while preserving the user's own list name on Screen 2 (user data, not a translated label).
