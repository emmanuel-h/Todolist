# Two things nobody could find: the date, and the way to add an item

_[#67](https://github.com/emmanuel-h/Todolist/issues/67) and
[#69](https://github.com/emmanuel-h/Todolist/issues/69) · Phase 2.5 of
[the device-feedback plan](device-feedback-plan.md)_

Two reports, one shape: an affordance that existed and could not be seen.

---

## #67 — "Do not understand how to enable due date. Tutorial is not enough."

A committed list row carried no date marks at all — only the date it already had, if it had
one. So there was no mark anywhere on the page saying that a list *can* have a day. The only
route was the edit sheet, which before [#72](row-controls.md) meant knowing that a sideways
drag opened an editor. The tour did play the beat, once, on the add line, as a hand miming a
gesture — and the reporter watched it and still could not find the feature.

**The date slot holds either the calendar button or the date itself.**

```
  |  Groceries        3 [/] [#] [X]  |   <- no date: a calendar button
  |  Errands   5 Sep  7 [/]     [X]  |   <- has a date: the jot, already pressable
```

It costs no extra width, because a list holds a target date **or** a due date and never both
— so the slot is never contested and **no row ever carries four controls**.

Pressing the button opens the same paper calendar the jot opens, through the same
`rewriteDate` path, with no date on it yet. The calendar sheet carries both marks and the
`DateKindCaption`, so the kind is chosen there and is explained **in words** — "To do on this
day" / "Finish before this day". That caption is the one place three wordless iterations
failed, and it is exactly why it had to survive the tutorial's deletion.

The button is a row control like the pencil and the bin and matches them: `InkIconButton` on
the rule, `InkTone.Margin` at rest, `InkTone.Words` pressed. It carries a new translated
description — "Give this list a day" / "Donner un jour à cette liste" — because
`set_target_date` would be a lie: pressing it does not commit to a kind, it opens the place
where the kind is chosen.

---

## #69 — "In a list, items addition with `...` is not very visible"

The complaint understates it. `InkAddLine` was an item **inside** the `LazyColumn`, so on a
page long enough to scroll the add line was not merely faint — it was **off-screen**. The
reporter's own nine-item list only showed the `…` after scrolling.

**Pinned, and marked.**

```
     ( )  Rework the logo        <- Ink, full
     ------------------------------------
     [+]  Add an item            <- mark: Ink, full
          ^^^^^^^^^^^            <- word: Margin, faint
```

The mark carries the visibility; the word stays faint so it does not read as a row somebody
wrote. Once the pen is on the paper the label gives way to what is being written and the `…`
ghost hint takes over, as it always did.

**The mark is the plus, not a pen.** The plan sketched a pen, but since #72 the pencil means
*edit* on every row, and a second writing implement three rules below it would collide. The
plus already means "a new one" on the page of lists, where it is the sticky pad's own mark.
One mark, one meaning, across both pages.

**It is a strip of the page, not a bar floating over it.** It carries the page's own ground
and its own rule, so rows scroll underneath and are covered. Transparent, it was a bug you
could see: a row passing behind the line drew its ring straight through the plus. It rides
above the keyboard.

---

## Words

Both pages now draw a word they did not before, and both are deliberate. `IconOnlyUiTest`,
the guard on which words each screen draws, was updated to name them exactly — not loosened.

One point of care: the drawn label and the field's own `contentDescription` are the same
words, and it is tempting to silence one of them. Silencing the **field** is wrong — the spec
requires every text field to carry a description, and a screen reader handed a loose label
and an unnamed field beside it cannot tell that the one belongs to the other. The field keeps
its name; the words repeat, which costs a moment and saves the affordance.

## Gates

1 081 tests green; 100% line and branch on `domain/` and `presentation/`; Pitest 196/196
mutants killed; `:app:lintDebug` 0 errors.

`ui/` is outside the mutation gate, so both were driven on a device: a dateless row draws the
button and a dated row draws the jot and never both; the button opens the calendar, the alarm
mark switches it to a due date and raises the caption, and picking a day swaps the button for
the jot; and with twelve items on the page, scrolled to the bottom and again with the
keyboard up, `＋ Add an item` stays on screen with nothing bleeding through it.
