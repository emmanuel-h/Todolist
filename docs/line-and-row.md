# The line and the row become two measures

_[#68](https://github.com/emmanuel-h/Todolist/issues/68) · landed 2026-09-02 · Phase 2.1 of
[the device-feedback plan](device-feedback-plan.md)_

> Multiline items should have a smaller interline size

```
  BEFORE                                AFTER
  ______________________________        ______________________________
   ( ) Multiline items should             ( ) Multiline items should
                                          have a smaller interline
  ______________________________        ______________________________
   have a smaller interline
                                        ______________________________
  ______________________________         ( ) Add colors
   ( ) Add colors                       ______________________________

   62px between an item's own lines,     62px inside an item,
   62px to the next item                 123px to the next item
```

## What was actually wrong

Not the size of the gap — the *ratio*. Measured on a Nokia X30 5G: an item's wrapped second
line sat 100px below its first, and the next item also sat 100px below. Identical. A wrapped
item was typographically indistinguishable from two items, and no amount of shrinking could
change that.

The plan's first answer was to take the page pitch from 56dp to 48dp. It was built, driven on
the phone, and **reverted**: everything got 14% smaller and the ratio was exactly where it
started. That is recorded here because the measurement is the useful part — the complaint is
about a ratio, and only a change that breaks the equality can answer it.

## The cause

One number did two jobs. `LocalPagePitch` was both the writing's leading *and* the row's
height, so intra-item spacing was inter-item spacing by construction.

## The fix

Split it.

- **Line pitch — 28dp.** The leading, and the spacing of the ruling. `PaperType.base.lineHeight`.
- **A row — its written lines plus one blank rule.** One line is two pitches (56dp, exactly
  today's row height, so single-line pages did not move); a wrapped one is three (84dp, down
  from 112dp).

Always: **28dp inside an item, 56dp to the next.** Every written line still sits on a rule,
an item's lines on consecutive rules, the next item a rule further down — which is how a
person writes a paragraph on ruled paper and skips a line before the next one.

`Modifier.pitchHeight` gained an `extraRules` parameter to express "and one blank rule";
`pagePitch()` lost its `TOUCH_FLOOR`, because the pitch is a typographic measure now and has
to be free to be smaller than a finger.

## Why 28

Because it survives the font-scale curve. Above scale 1.05 the platform stops converting `sp`
linearly and compresses larger sizes hardest, so the three hands (14/18/20sp) can resolve to
different line boxes and the writing drifts off the rules. Sweeping the candidates against
scales 1.0 → 2.0, measuring whether all three hands still land on exactly one line box:

| leading | holds at every sampled scale? |
|---|---|
| 56 (before) | yes |
| **28** | **yes** |
| 30 | yes |
| 34 | no — breaks at 1.3 |
| 48 | no — breaks at 1.3 |
| 26 | no — breaks at 1.0, 1.15, 1.8, 2.0 |

`RuledHandTest`'s `should hold every hand to one line of the page when the font scale bends`
is the guard. The 1px divergences above are a latent rounding artefact of the `em`-based
leading in `lineBoxOf`; 56 happened to mask it, 28 also masks it, and it is still there for
some other value to find.

## The trap: a rule is smaller than a finger

At 28dp every control written on a rule became a 28dp tap target — the ring, the pencil, the
bin, the date jot. Flooring their heights to 48dp does not work: it makes the row's *content*
taller than one rule, so the row rounds up to a second rule of writing it does not have and
every row on the page grows by half again.

`Modifier.pressableBelowTheRule` resolves it. The control stays one rule tall to everything
that measures it and reaches down into the blank rule the row already carries for everything
that presses it — Compose does not clip hit-testing to parent bounds. `RowTouchTargetTest`
pins both halves at once: ≥48dp of touch, **and** a one-line row still exactly two rules.

The completion ring needed one more thing: it was seated at the bottom of its own box, which
once the box reached past the rule dropped it onto the blank rule below its own writing. It
is seated on the rule now, inside the taller press area.

## Gates

1 076 tests green; 100% line and branch on `domain/` and `presentation/`; Pitest 196/196
mutants killed; `:app:lintDebug` 0 errors. `ui/` is outside the mutation gate, so this was
driven on a device and measured from the screen: 62px inside an item, 123px to the next.
