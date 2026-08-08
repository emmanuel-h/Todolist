# List Due Date

## What it does
A list can optionally carry a due date — a deadline by which its items must be done. The due date is displayed on the list row with an alarm-clock icon and is tinted in three tiers (future / today / overdue) against the current date. It can be set or cleared both at creation time and via the rename dialog, and it is mutually exclusive with the target date: setting one kind always clears the other.

## Architecture
- **Layers**: domain, data, presentation, ui
- **Key types**:
  - `DueDateStatus` — `FUTURE / TODAY / OVERDUE` enum; computed by the use case against a `Clock`
  - `TodoList.dueDate: LocalDate?` — nullable due date on the domain model; `init {}` enforces mutual exclusion with `targetDate` via `require`
  - `TodoListSummary.dueDateStatus: DueDateStatus?` — `null` when no due date is set; `FUTURE`, `TODAY`, or `OVERDUE` otherwise
  - `TodoListSummary.showDueDateYear: Boolean` — `true` when `dueDate.year != today.year`
  - `GetTodoListsWithStatusUseCase` — computes `dueDateStatus` and `showDueDateYear` per list alongside the existing target-date flags
  - `CreateTodoListUseCase` — gains `dueDate: LocalDate? = null`; enforces mutual exclusion with `targetDate` via `require`
  - `EditTodoListUseCase` — gains `dueDate: LocalDate?`; enforces mutual exclusion; calls `updateDueDate` on the repository
  - `TodoListRepository.updateDueDate(todoListId, dueDate)` — new contract method
  - `TodoListEntity.dueDate: Long?` — persisted as epoch day; `MIGRATION_6_7` adds the column
  - `TodoListsAdapter.bindDueDate()` — applies three-tier tinting via `resolveTintColor()`
- **Async contract**: same `Flow`-based contract as the existing status use case; `dueDateStatus` is recomputed whenever the repository emits → `→ see list-target-date.md`

## Files
- `app/src/main/java/fr/mandarine/todolist/domain/DueDateStatus.kt` — new `FUTURE / TODAY / OVERDUE` enum
- `app/src/main/java/fr/mandarine/todolist/domain/TodoList.kt` — added `dueDate: LocalDate?`; `init {}` enforces mutual exclusion
- `app/src/main/java/fr/mandarine/todolist/domain/TodoListSummary.kt` — added `dueDateStatus: DueDateStatus?` and `showDueDateYear: Boolean`
- `app/src/main/java/fr/mandarine/todolist/domain/TodoListRepository.kt` — added `updateDueDate(todoListId, dueDate)` contract
- `app/src/main/java/fr/mandarine/todolist/domain/CreateTodoListUseCase.kt` — added `dueDate: LocalDate? = null`; enforces mutual exclusion
- `app/src/main/java/fr/mandarine/todolist/domain/EditTodoListUseCase.kt` — added `dueDate: LocalDate?`; enforces mutual exclusion; calls `updateDueDate`
- `app/src/main/java/fr/mandarine/todolist/domain/GetTodoListsWithStatusUseCase.kt` — computes `dueDateStatus` and `showDueDateYear` per list
- `app/src/main/java/fr/mandarine/todolist/data/TodoListEntity.kt` — added `dueDate: Long?` column (epoch day)
- `app/src/main/java/fr/mandarine/todolist/data/TodoListDao.kt` — added `updateDueDate` `@Query`
- `app/src/main/java/fr/mandarine/todolist/data/RoomTodoListRepository.kt` — implemented `updateDueDate`; maps epoch day in `getAll()` and `add()`
- `app/src/main/java/fr/mandarine/todolist/data/InMemoryTodoListRepository.kt` — implemented `updateDueDate`
- `app/src/main/java/fr/mandarine/todolist/data/TodoDatabase.kt` — bumped to version 7; added `MIGRATION_6_7` (`ALTER TABLE todo_lists ADD COLUMN dueDate INTEGER`)
- `app/src/main/java/fr/mandarine/todolist/presentation/TodoListsViewModel.kt` — `createList` and `editList` both gain `dueDate: LocalDate? = null`
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListsAdapter.kt` — added `bindDueDate()` with three-tier tinting
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListsActivity.kt` — added `selectedInlineDueDate` and `selectedRenameDueDate`; rewired inline add row and rename dialog with mutual-exclusion logic; `updateRenameDateDisplay()` drives the toggle selection and `textDialogDate`; `resolveTintColor()` helper; switching the toggle moves an existing date across to the new kind without opening the picker
- `app/src/main/res/drawable/ic_alarm.xml` — alarm clock vector drawable (clock face + two bells + clock hand)
- `app/src/main/res/values/attrs.xml` — declares `colorWarning` custom theme attribute
- `app/src/main/res/values/colors.xml` — added `md_theme_light_warning` (#B45309) and `md_theme_dark_warning` (#F59E0B)
- `app/src/main/res/values/themes.xml` and `res/values-night/themes.xml` — map `colorWarning` for light and dark
- `app/src/main/res/values/strings.xml` — added `set_due_date` and `clear_due_date` content-description strings
- `app/src/main/res/layout/item_todo_list.xml` — added `layoutDueDate` group (alarm icon + text, `gone` by default) below `layoutTargetDate`
- `app/src/main/res/layout/item_todo_list_inline_add.xml` — added `btnListInlineDueDate` (alarm icon) between the calendar button and the submit button
- `app/src/main/res/layout/dialog_rename_list.xml` — contains a `MaterialButtonToggleGroup` (`toggleDateKind`, `singleSelection` + `selectionRequired`) with two icon-only outlined buttons (`btnToggleTargetDate` calendar, `btnToggleDueDate` alarm) for mode selection, and a boxed outlined `MaterialCardView` (`layoutDateBox`) that shows the formatted date (via `textDialogDate`) or an add-affordance icon when no date is set; `layoutDateBox` is the sole trigger for `DatePickerDialog`; the clear button (`btnDialogClearDate`) is visible only when a date is set
- `app/src/test/java/fr/mandarine/todolist/ui/TodoListsDueDateTest.kt` — 35 Robolectric tests (row show/hide, all three tint tiers, create-with-due-date, mutual exclusion in both surfaces, toggle mode selection, date-carry-on-kind-switch, clear-leaves-mode-checked, default-calendar-mode, `colorError` never on target date)
- `app/src/test/java/fr/mandarine/todolist/ui/TodoListsTargetDateTest.kt` — 30 Robolectric tests; updated for the toggle+boxed-date dialog; use `btnToggleTargetDate` / `btnToggleDueDate` and `MaterialButtonToggleGroup.checkedButtonId` in place of the removed image-view IDs
- 8 new domain/data/presentation test classes covering the domain invariant, use cases, both repositories, the entity, the migration, and the ViewModel

## Invariants & contracts
- **Mutual exclusion is enforced at the domain layer**: `TodoList.init` throws if both `targetDate` and `dueDate` are non-null. `CreateTodoListUseCase` and `EditTodoListUseCase` also `require` that at most one is set. The UI layer must clear the opposite kind before calling through.
- `DueDateStatus` is computed by the use case against the injected `Clock`; the UI layer must never inspect the current date directly to determine overdue status.
- `colorError` must only be applied to due dates with `OVERDUE` status; it must never be applied to target dates or to `TODAY` / `FUTURE` due dates.
- `colorWarning` applies exclusively to `TODAY` due dates; it must never be applied to target dates.
- `dueDate` is stored as epoch day (`Long?`) via `LocalDate.toEpochDay()` / `LocalDate.ofEpochDay()` — never as timestamp millis or a formatted string.
- `MIGRATION_6_7` must remain registered; dropping it will crash users on database version 6.
- The `showDueDateYear` flag is set by the use case; the adapter must use this flag rather than comparing years directly.
- The due date does not affect list sort order. Drag-to-reorder positions are fully preserved regardless of due-date state.
- In the rename dialog, tapping a mode button in `toggleDateKind` switches the active kind only — it never opens `DatePickerDialog`. The boxed date field (`layoutDateBox`) is the sole trigger for the picker.
- Switching the toggle while a date is already set moves that date to the newly selected kind, preserving the mutual-exclusion invariant without requiring the user to re-enter the date.
- Clearing the date via `btnDialogClearDate` removes the date but leaves the toggle on its current selection; the default selection when opening the dialog with no date is target date (calendar).
- The checked button in `toggleDateKind` is rendered with a filled `colorSecondaryContainer` container; state is conveyed by shape and fill, not colour alone.

## UI
- **Screen(s)**: `TodoListsActivity`
- **Layout file(s)**: `res/layout/item_todo_list.xml`, `res/layout/item_todo_list_inline_add.xml`, `res/layout/dialog_rename_list.xml`
- **Design decisions**: Three-tier tinting (`colorPrimary` / `colorWarning` / `colorError`) rather than a binary approach makes the urgency gradient legible at a glance without any text label, preserving the icon-only UI rule. `colorWarning` is a new custom attribute because Material 3 has no built-in warning semantic color token. The rename dialog separates mode selection from date selection: `toggleDateKind` controls which kind is active while `layoutDateBox` is the sole trigger for `DatePickerDialog`, so the user can switch kind without being forced to re-pick a date. The inline add row (`item_todo_list_inline_add.xml`) intentionally keeps the original one-tap behavior — tapping the calendar or alarm icon there opens the picker directly — for fast entry at creation time. The two surfaces behave differently by deliberate design choice, not inconsistency.
