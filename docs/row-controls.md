# The row grows controls, and the swipe goes

_[#72](https://github.com/emmanuel-h/Todolist/issues/72) · landed 2026-09-02 · Phase 2.2 of
[the device-feedback plan](device-feedback-plan.md)_

```
  BEFORE                                AFTER
  +----------------------------------+  +----------------------------------+
  |   v >                     < X    |  |  ( )  Buy bread          [/] [X] |
  |  ( )  Buy bread                  |  |  ( )  Milk               [/] [X] |
  |                                  |  |                                  |
  |   v >                     < X    |  |                                  |
  |  ( )  Milk                       |  |                                  |
  +----------------------------------+  +----------------------------------+

     two half-glyphs per row at 0.6      two controls per row at full ink,
     ink, a rule above the words         on the rule they belong to
     they belong to
```

## Why

Every destructive or secondary act on a row was a horizontal drag. The hint that a drag
existed was a mark and a chevron at each end of every row, drawn at `REST_INK = 0.6f`. On
the reporter's own nine-item list that is eighteen faint half-glyphs, and they read as
smudges rather than as controls — which is visible in the screenshot their list produced.

The hint had already been iterated on three times and had not landed. `CornerMark.kt`'s own
comment admitted the starting position: *"nothing at rest used to say so: the mark under it
was drawn only once the finger was already moving, so the gesture had to be guessed before
it could be seen."* The report asked to stop iterating on the hint and make the acts real
controls.

## What a row carries now

- **Item row** — `( ) title [pencil] [bin]`. The pencil opens the in-place editor; so does
  tapping the title, which stays as the fast route. The bin tears the row off.
- **List row** — `name · dates · count [pencil] [bin]`. The pencil opens the rename sheet —
  which used to be reachable only by a swipe, and was the only way to rename a list at all.
  The bin tears the row off.

The 52dp `CORNER_ROOM` spacer that used to hold the corner marks is what became the
controls. Two 48dp buttons cost about 44dp more than that spacer, which comes off the title
column. A dateless list row's name goes from roughly 250dp to 205dp on a 360dp screen.

**No calendar button.** The date slot on a list row is
[#67](https://github.com/emmanuel-h/Todolist/issues/67); the trailing group is built to take
a third control without moving the other two.

## At rest and pressed

`InkIconButton` already seated a glyph on a rule, already raised `PaperFocusMark`, and
already squashed the nib on press. It gained one optional parameter rather than being
forked:

| | at rest | pressed |
|---|---|---|
| ink | `InkTone.Margin`, full alpha | `InkTone.Words` |
| indication | — | `PaperFocusMark` |
| haptic | — | `PaperHaptics.pickUp()` |

Full ink, not the old 0.6 — these are controls now, not a hint that a gesture exists. The
`pressedTint` parameter defaults to null, so the calendar chevrons, the back glyph and the
date marks behave exactly as they did.

## What survives

- The **ring's tap** ticks an item.
- The **title's tap** opens the editor on an item and opens the list on a list row.
- **Long-press-and-drag reorders.** A different gesture, deliberately out of scope, and
  still the only way to reorder — which is why `RowVerbs` keeps `move_up` / `move_down` as
  TalkBack custom actions. The verbs for edit, delete and toggle came off: those are
  focusable buttons now, and naming them twice gave a screen reader the same verb from two
  places.
- The **tear-off animation** and the **9-second undo slip**. The bin feeds exactly what the
  swipe fed. Replacing the slip with a confirmation is
  [#66](https://github.com/emmanuel-h/Todolist/issues/66).

## What went

`ui/paper/SwipeRow.kt` and `ui/paper/CornerMark.kt`, whole, with `SwipeMark`, `SwipeReveal`,
`RowSwipe` and `RowSwipeState`. `RowSwipeStateTest` went with them, and the swipe tests in
`PaperPrimitivesTest`. Nothing under `app/src/` mentions a swipe any more.

## A note on the tests

Several tests drove a swipe to reach an act. None was deleted — each was rewritten to press
the button, because the behaviour still exists and only the way in changed. Two tests that
tapped a row to open it had to start aiming at the name rather than at the row's centre:
with the name column 44dp narrower, the centre of a dated row now falls on the date jot,
which has its own press and opens the calendar. That is the jot behaving as specified, not a
regression.

## Gates

1 072 tests green; 100% line and branch on `domain/` and `presentation/`; Pitest 196/196
mutants killed; `:app:lintDebug` 0 errors. `ui/` is outside the mutation gate, so it was also
driven on an emulator: pencil opens the editor and the rename sheet, bin tears the row and
raises the undo slip, the title still opens the list.
