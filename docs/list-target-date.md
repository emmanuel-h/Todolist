# List Target Date

## What it does
The user can assign an optional informational target date to any list (e.g. "things to do next Sunday") at creation time or via the rename dialog. The date is displayed on each list row below the list name; dates in the past are shown with a muted `colorOnSurfaceVariant` tint rather than an error tint. No sort order is affected.

## Architecture
- **Layers**: domain, data, presentation, ui
- **Key types**:
  - `TodoList.targetDate: LocalDate?` — nullable target date on the domain model
  - `TodoListSummary.isTargetDateElapsed: Boolean` — `true` when `targetDate.isBefore(today)`; computed in the use case against a `Clock`
  - `TodoListSummary.showTargetYear: Boolean` — `true` when `targetDate.year != today.year`; keeps the UI unaware of the current date
  - `GetTodoListsWithStatusUseCase(clock: Clock)` — now requires a `Clock` injection; computes both elapsed and show-year flags
  - `CreateTodoListUseCase` — gains `targetDate: LocalDate? = null` parameter
  - `EditTodoListUseCase` — gains `targetDate: LocalDate?` parameter; calls both `updateName` and `updateTargetDate` on the repository
  - `TodoListRepository.updateTargetDate(todoListId, targetDate)` — new contract method
  - `TodoListEntity.targetDate: Long?` — persisted as epoch day (`LocalDate.toEpochDay()` / `LocalDate.ofEpochDay()`)
  - `MIGRATION_5_6` — adds `targetDate INTEGER` (nullable) column to `todo_lists`
  - `TodoListsAdapter.formatTargetDate(date, showYear, locale)` — static helper; uses `DateFormat.getBestDateTimePattern` with skeleton `EEEdMMM` or `EEEdMMMy` for locale-correct output
- **Async contract**: synchronous; same computed-property contract as before (→ see `all-done-list-status.md`)

## Files
- `app/src/main/java/fr/mandarine/todolist/domain/TodoList.kt` — added `targetDate: LocalDate?`
- `app/src/main/java/fr/mandarine/todolist/domain/TodoListSummary.kt` — added `isTargetDateElapsed` and `showTargetYear`
- `app/src/main/java/fr/mandarine/todolist/domain/TodoListRepository.kt` — added `updateTargetDate(todoListId, targetDate)` contract
- `app/src/main/java/fr/mandarine/todolist/domain/CreateTodoListUseCase.kt` — added `targetDate: LocalDate? = null` parameter
- `app/src/main/java/fr/mandarine/todolist/domain/EditTodoListUseCase.kt` — added `targetDate: LocalDate?`; calls both `updateName` and `updateTargetDate`
- `app/src/main/java/fr/mandarine/todolist/domain/GetTodoListsWithStatusUseCase.kt` — added `Clock` injection; computes `isTargetDateElapsed` and `showTargetYear` per list
- `app/src/main/java/fr/mandarine/todolist/data/TodoListEntity.kt` — added `targetDate: Long?` (epoch day)
- `app/src/main/java/fr/mandarine/todolist/data/TodoListDao.kt` — added `updateTargetDate` `@Query`
- `app/src/main/java/fr/mandarine/todolist/data/RoomTodoListRepository.kt` — implemented `updateTargetDate`; maps `Long?` <-> `LocalDate?` via epoch day
- `app/src/main/java/fr/mandarine/todolist/data/InMemoryTodoListRepository.kt` — implemented `updateTargetDate`
- `app/src/main/java/fr/mandarine/todolist/data/TodoDatabase.kt` — bumped to version 6; added `MIGRATION_5_6`
- `app/src/main/java/fr/mandarine/todolist/presentation/TodoListsViewModel.kt` — `createList(name, targetDate?)`, `editList(id, name, targetDate?)`, inline submit passes `null`
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListsActivity.kt` — passes `SystemClock()` to use case; `wireInlineAddRow()` wires `btnListInlineDate` -> `DatePickerDialog` and manages `ic_event_add`/`ic_event` icon swap; inline submit calls `createList(name, selectedInlineDate)`; `showRenameDialog` initialises `selectedRenameDate` from `list.targetDate`, wires date-row tap and clear button; `updateRenameDateDisplay()` toggles only the plus icon, date text and clear button — the calendar icon visibility is fixed in XML and never touched in code
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListsAdapter.kt` — `ViewHolder` gains target-date views; `bindTargetDate()` applies `colorOnSurfaceVariant` (elapsed) / `colorPrimary` (active or no date); static `formatTargetDate()` uses ICU `getBestDateTimePattern`
- `app/src/main/res/drawable/ic_event.xml` — new Material Design calendar/event vector icon
- `app/src/main/res/drawable/ic_event_add.xml` — composite vector: calendar frame with a centred plus glyph; used as the "add a date" affordance wherever no date is set yet; tinted at the usage site (same `fillColor` convention as `ic_event.xml`)
- `app/src/main/res/layout/item_todo_list.xml` — `textListName` wrapped in vertical `LinearLayout`; hidden `layoutTargetDate` second line with `iconTargetDate` (14dp) + `textTargetDate`; slot reserved for issue #8 due-date pair
- `app/src/main/res/layout/item_todo_list_inline_add.xml` — added `btnListInlineDate` between text field and submit button; `MaterialButton` has a single icon slot so uses the combined `ic_event_add` glyph in the empty state, swaps to `ic_event` once a date is selected, resets to `ic_event_add` on clear/cancel/submit
- `app/src/main/res/layout/dialog_rename_list.xml` — added `layoutDialogDate` (clickable row with ripple, `contentDescription="@string/set_target_date"`), `iconDialogTargetDate` (calendar, 20dp, **permanently visible** in both states, `marginEnd` 4dp), `iconDateAddAffordance` (`ic_add`, 16dp, toggles: `VISIBLE` in empty state, `GONE` in filled state), `textDialogTargetDate` (starts `GONE`), and `btnDialogClearDate` (hidden until a date is set); empty state shows calendar + subordinate plus; filled state shows calendar + formatted date + clear button
- `app/src/main/res/values/strings.xml` — added `set_target_date` and `clear_target_date` content-description strings
- `app/src/test/java/fr/mandarine/todolist/domain/CreateTodoListUseCaseTargetDateTest.kt` — tests for `targetDate` propagation through create
- `app/src/test/java/fr/mandarine/todolist/domain/EditTodoListUseCaseTargetDateTest.kt` — tests for `targetDate` propagation through edit
- `app/src/test/java/fr/mandarine/todolist/domain/GetTodoListsWithStatusUseCaseElapsedTest.kt` — elapsed and show-year flag computation tests
- `app/src/test/java/fr/mandarine/todolist/data/InMemoryTodoListRepositoryTargetDateTest.kt` — in-memory `updateTargetDate` tests
- `app/src/test/java/fr/mandarine/todolist/data/RoomTodoListRepositoryTargetDateUnitTest.kt` — unit-level mapping tests
- `app/src/test/java/fr/mandarine/todolist/data/RoomTodoListRepositoryTargetDateTest.kt` — Robolectric integration tests
- `app/src/test/java/fr/mandarine/todolist/data/TodoListEntityTargetDateTest.kt` — epoch-day conversion tests
- `app/src/test/java/fr/mandarine/todolist/data/TodoDatabaseMigration5to6Test.kt` — migration tests
- `app/src/test/java/fr/mandarine/todolist/presentation/TodoListsViewModelTargetDateTest.kt` — ViewModel wiring tests
- `app/src/test/java/fr/mandarine/todolist/ui/TodoListsTargetDateTest.kt` — 30 Robolectric tests covering date show/hide, elapsed vs future tint, year/no-year, ICU locale, inline plus/calendar icon swap, create-with-date, clear-via-dialog, set-via-dialog, clear-button visibility, plus-icon/calendar-icon/text toggle visibility in dialog empty and filled states

## Invariants & contracts
- `targetDate` is purely informational and must never influence sort order on the lists screen.
- `isTargetDateElapsed` is computed by the domain use case against a `Clock`; the UI layer must never inspect the current date directly.
- `showTargetYear` is computed by the domain use case; the adapter must use this flag rather than comparing `LocalDate.year` to the current year.
- An elapsed target date receives `colorOnSurfaceVariant` tint (muted). `colorError` tint is deliberately reserved for issue #8 (overdue due date); do not apply it to elapsed target dates.
- `targetDate` is stored as epoch day (`Long?`) in Room; always convert via `LocalDate.toEpochDay()` / `LocalDate.ofEpochDay()`. Never store as a formatted string or timestamp millis.
- `MIGRATION_5_6` must remain registered; dropping it will crash users on database version 5.
- `formatTargetDate()` uses `DateFormat.getBestDateTimePattern` with skeleton `EEEdMMM` (no year) or `EEEdMMMy` (with year) and `Locale.getDefault(Locale.Category.FORMAT)`; never substitute a hardcoded pattern.
- The `layoutTargetDate` second line in `item_todo_list.xml` has a reserved empty slot to the right of the date for the issue #8 due-date icon pair; do not remove it.
- `btnDialogClearDate` in the rename dialog must remain hidden (`GONE`) until a date is actually set; show it only when `selectedRenameDate != null`.
- The date row empty state in the rename dialog must be icon-only: do not reintroduce a text placeholder. A string-resource placeholder would both violate the icon-only UI rule and re-create the coupling bug where `R.string.list_name_hint` was being reused for a semantically unrelated purpose.
- `iconDialogTargetDate` (calendar) must remain `VISIBLE` in both empty and filled states — its visibility is fixed in XML and must not be toggled in code. `iconDateAddAffordance` (plus) is the only element that toggles between states; the plus must never appear without the calendar beside it.
- `GetTodoListsWithStatusUseCase` must receive a `Clock` at construction time; `TodoListsActivity` passes `SystemClock()` as the production implementation.

## UI
- **Screen(s)**: `TodoListsActivity`
- **Layout file(s)**: `res/layout/item_todo_list.xml`, `res/layout/item_todo_list_inline_add.xml`, `res/layout/dialog_rename_list.xml`
- **Design decisions**: Elapsed dates use `colorOnSurfaceVariant` rather than `colorError` to signal "the day has passed" rather than "a deadline was missed" — the stronger error signal is deliberately held in reserve for issue #8. The year is omitted when the target date falls in the current year, reducing visual noise for the common case. ICU skeleton-based formatting via `getBestDateTimePattern` ensures field order is locale-correct (e.g. `Sun 22 Jun` en-GB vs `dim. 22 juin` fr-FR vs `So., 22. Juni` de-DE) with zero string resources.
