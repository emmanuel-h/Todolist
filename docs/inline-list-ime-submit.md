# Inline List Create — IME Key Submit Fix

## What it does
Fixes the inline list-name field on the lists screen so that pressing the keyboard's primary action key (OK / Done / Enter / checkmark) commits the new list, regardless of which action ID the IME dispatches. Before this fix only `IME_ACTION_DONE` was handled; now `IME_ACTION_UNSPECIFIED` (Samsung and third-party keyboards) and raw `KEYCODE_ENTER ACTION_DOWN` events (physical keyboards and some IMEs) also trigger submission.

## Architecture
- **Layers**: ui only (no domain, data, or ViewModel changes)
- **Key types**: none introduced
- **Async contract**: none

## Files
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListsActivity.kt` — extended `wireInlineAddRow()` editor-action listener to handle `IME_ACTION_DONE`, `IME_ACTION_UNSPECIFIED`, and `KEYCODE_ENTER ACTION_DOWN`; added `triggerImeActionForTest(actionId: Int)` test helper
- `app/src/test/java/fr/mandarine/todolist/ui/TodoListsInlineAddTest.kt` — 6 new Robolectric tests: submit via `IME_ACTION_DONE`, submit via `IME_ACTION_UNSPECIFIED`, row hides after IME submit, blank rejected for both action IDs, unrelated action ID (`IME_ACTION_NEXT`) ignored

## Invariants & contracts
- `wireInlineAddRow()` must handle all three conditions in one `setOnEditorActionListener`: `IME_ACTION_DONE`, `IME_ACTION_UNSPECIFIED`, and `event.keyCode == KEYCODE_ENTER && event.action == ACTION_DOWN`; do not remove any of these cases.
- Blank input must be rejected silently for every IME path; the inline row must remain visible in that case.
- Unrelated action IDs (e.g. `IME_ACTION_NEXT`) must return `false` from the listener and leave the list unchanged.
- The submit button tap path is unaffected by this change (→ see `inline-list-create.md`).

## UI
- **Screen(s)**: `TodoListsActivity`
- **Layout file(s)**: none changed
- **Design decisions**: no visual change; this fix is keyboard behaviour only
