# UI Polish

## What it does
Improves the visual quality of both screens: illustrated empty states replace blank views, the inline add bar is fixed to the bottom of the list, M3 colour tokens replace hardcoded colours, and dark mode is supported via a night-override theme.

## Architecture
- **Layers**: ui only (no domain or data changes)
- **Key types**: no new types; changes are confined to layout XML, theme files, and adapter rendering logic
- **Async contract**: none

## Files
- `app/src/main/res/layout/activity_todo_lists.xml` — added empty-state illustration (`ic_checklist`) shown when `TodoListsState.Empty`
- `app/src/main/res/layout/activity_todo_list.xml` — fixed inline add row positioning; empty-state illustration for lists with no items
- `app/src/main/res/layout/item_todo_inline_add.xml` — refined TextInputEditText + send icon layout
- `app/src/main/res/drawable/ic_checklist.xml` — vector illustration used for empty states on both screens
- `app/src/main/res/drawable/ic_arrow_forward.xml` — forward-arrow icon for list navigation affordance
- `app/src/main/res/values/colors.xml` — M3 colour token definitions
- `app/src/main/res/values/themes.xml` — `Theme.Material3.DayNight.NoActionBar` base; M3 token assignments
- `app/src/main/res/values-night/themes.xml` — dark-mode override theme
- `app/src/main/res/values/dimens.xml` — shared dimension tokens (margins, icon sizes)
- `app/src/main/res/values/strings.xml` — string resources for empty-state labels and dialog hints
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListsActivity.kt` — empty-state visibility toggling
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListActivity.kt` — empty-state visibility toggling, keyboard-dismiss touch handling
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListAdapter.kt` — rendering fixes for inline add bar view type

## Invariants & contracts
- The empty-state illustration must be toggled via `View.VISIBLE` / `View.GONE` in the Activity whenever the UI state changes; it must not be driven from the adapter.
- All colours must reference M3 theme attributes (e.g. `?attr/colorPrimary`) rather than raw hex values; do not reintroduce hardcoded colours.
- `dialog_add_item.xml` is kept in the repository but is not wired to any user flow; do not remove it without checking for any future use, but do not add new call sites to it.

## UI
- **Screen(s)**: `TodoListsActivity`, `TodoListActivity`
- **Layout file(s)**: `res/layout/activity_todo_lists.xml`, `res/layout/activity_todo_list.xml`, `res/layout/item_todo_inline_add.xml`
- **Design decisions**: A single shared `ic_checklist` vector is reused for empty states on both screens to keep the visual language consistent. Dimension tokens in `dimens.xml` prevent magic-number duplication across layouts. Dark mode is implemented via a separate `values-night/themes.xml` override rather than dynamic colour to keep compatibility with min SDK 24.
