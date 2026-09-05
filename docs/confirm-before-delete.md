# Ask before a delete, instead of only offering to undo it

_[#66](https://github.com/emmanuel-h/Todolist/issues/66) · Phase 2.4 of
[the device-feedback plan](device-feedback-plan.md)_

> Have a confirmation before delete

```
  press [X] on "Groceries"

    +-------------------------+
    |  Delete "Groceries"?    |
    |  and the 2 items on it  |
    |                         |
    |    Cancel      Delete   |
    +-------------------------+

    Delete -> written at once
    Cancel -> nothing happened
```

## Why the undo slip was not enough

A delete was a one-way gesture with a nine-second grace period, not a question. The row tore
off, an undo slip stood in its place for `UNDO_SLIP_MILLIS`, and the write landed when the
slip expired, when another row was torn off, **or when the page left**.

That last one is the report. Tear a list off, walk out of the page inside those nine seconds,
and the list and every item on it were gone — with nothing having asked, and the slip that
would have offered the undo gone with the page. The `delete_list` and `cancel` strings that
would have carried a question had been sitting in `strings.xml` unused the whole time.

## What replaced it

One decision point, before anything is written. The confirmation **replaces** the slip on
both pages rather than sitting in front of it — two grace periods for one act is worse than
either alone.

- **A list's bin** asks `Delete "<name>"?` and, when the list holds any items, adds
  `and the N items on it` — the cascade named before it happens. A list with nothing on it
  is asked about in one line; a line saying zero is noise.
- **An item's bin** asks the same question with no second line. An item takes nothing with it.
- **Delete** writes it through at once. **Cancel** leaves the page as it was — and so does
  back, and so does a tap on the veil outside the sheet. A delete happens when, and only
  when, Delete is pressed.

The count comes from `TodoListSummary.activeCount + completedCount`, both of which the page
already had, so nothing below `ui/` changed for this.

The prompt is a `PaperDialog` — a smaller sheet laid on the page with the page's own grain
and warm veil, like the rename sheet. Not a Material dialog.

## What went

`ui/DeletionState.kt` whole, with `PendingDeletion` and `UNDO_SLIP_MILLIS`; `UndoSlip` in
`ui/paper/TearOff.kt`; and both tests that covered them. `Modifier.tearOff` stays — a
confirmed delete still tears the row off the paper, it just has nothing to wait for
afterwards.

## The free simplification

Reorder used to reconcile two index spaces, because the page hid a torn-off row for the
length of its undo slip while the repository still held it. With no slip that disagreement
stops existing: `filterNot { deletion.hides(...) }` is gone from both screens and the page
shows exactly the rows the repository holds. The drag preview (`stageOrder` / `releaseOrder`)
is a different mechanism and is untouched.

## Words

Three new strings, in `values/` and `values-fr/`, and the cascade line is a real `<plurals>`
with French's own forms:

| | English | French |
|---|---|---|
| question | `Delete "%1$s"?` | `Supprimer « %1$s » ?` |
| confirm | `Delete` | `Supprimer` |
| cascade | `and the %d item(s) on it` | `et la/les %d tâche(s) qu'elle contient` |

`IconOnlyUiTest` — the guard on which words each screen draws — was updated on purpose, not
weakened: it pins the question, the cascade line and both button labels by their exact text,
and still pins the resting pages to exactly the words they drew before.

## Gates

1 073 tests green; 100% line and branch on `domain/` and `presentation/`; Pitest 196/196
mutants killed; `:app:lintDebug` 0 errors.

`ui/` is outside the mutation gate, so it was driven on a device: an empty list asks in one
line, a list with two items names them, an item asks with no cascade, Cancel and the veil and
back all leave the page untouched, Delete writes through and survives a cold restart, and a
confirmed list delete takes its items with it.
