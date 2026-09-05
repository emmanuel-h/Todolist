# A colour per list, on the paper and never on the ink

_[#71](https://github.com/emmanuel-h/Todolist/issues/71) · Phase 4.2 of
[the device-feedback plan](device-feedback-plan.md)_

> Add colors

## The question the plan left open

Colour in this app was already load-bearing. `inkBlue` means *acted on*, `inkAmber` *due
today*, `inkRed` *lost or being torn off* — and the spec forbids borrowing red. So a reader's
colour could not simply be another pen: a list painted red and a list overdue in red would be
the same mark meaning two things.

The plan named the likely answer and left it open until this phase. It is the answer:

**The reader's colour goes on the paper, never on the ink.** It is a highlighter wash behind
the list's name. Nothing that carries meaning is tinted — not a glyph, a rule, a date jot or
a numeral. A highlighter is the one tool that adds colour to paper without being ink, which
is why it fits an app made of ink and paper.

```
  ┌──────────────────────────────────────┐
  │ ▓▓Travel▓▓          [✎] [📅] [🗑]     │   the wash follows the words and
  │ ▓▓Garden▓▓          [✎] [📅] [🗑]     │   stops where they stop
  │ ▓▓Reading▓          [✎] [📅] [🗑]     │
  └──────────────────────────────────────┘
```

## A closed set, not a colour wheel

Six hues — Butter, Mint, Rose, Sky, Peach, Lilac — plus `None`.

The reason it is closed is `PaperPalette.night`, which is not `light` inverted but a second
stock of paper. Every hue therefore needs a **lamplit twin**, and a free ARGB value cannot
have one. The night washes are authored separately to read as tonal shifts in the paper
rather than as coloured light:

| Hue | Light | Night |
|---|---|---|
| Butter | `#FCEF88` | `#3F3820` |
| Mint | `#B2E8BC` | `#1C3E26` |
| Rose | `#FFCAD0` | `#3E1C22` |
| Sky | `#BDD8F5` | `#18203E` |
| Peach | `#FFD9A8` | `#3E2418` |
| Lilac | `#DCC8F5` | `#241838` |

The daylight washes are pre-mixed at about 40% over the paper tone: the least that makes a
hue unambiguous while leaving the ink above 7:1 contrast on every one of them. Legibility
set the alpha, not taste.

## The picker

Seven marks on the **edit sheet** — where a list's name and dates are already changed — with
the chosen one **circled in ink**, the same mark the paper calendar rings a day with and the
hour grid rings an hour with. `None` reads as bare paper.

Not on the add line: it already carries two date marks and a commit tick, and a new list can
be coloured the moment it exists.

## The trap that would have eaten it

`EditTodoListUseCase` and `TodoListsViewModel.editList` take the colour with a **default of
`None`**. Every call site in `ui/` predates colour and passed none — so renaming a list, or
writing a date on it from the row or from the items page's head rule, would have silently
stripped the reader's colour. Three call sites in `PageStack` plus the screen's date writer
now pass the list's current colour, and two tests pin it: renaming a coloured list leaves it
coloured, and so does writing a date on one. A test that only exercised the picker would have
passed the whole time while the app quietly bleached every list the reader renamed.

## Shape

`domain/ListColour` is an enum and names no ARGB value — what a hue looks like, and looks like
by lamplight, is `PaperPalette`'s business. `data/` stores the enum's **name** as TEXT rather
than its ordinal, so reordering the enum can never silently reinterpret stored rows.
Migration 7 → 8 adds the column with `DEFAULT 'None'`; every existing list comes out
uncoloured, and there is still no destructive fallback.

## Gates

1 209 tests green; 100% line and branch on `domain/`, `data/` and `presentation/`; Pitest
201/201 mutants killed; `:app:lintDebug` 0 errors. Driven on a device in **both sheets**: six
lists each painted a different hue, ink legible on every wash in daylight and by lamplight,
the page scannable at a glance, and a rename leaving the colour where it was.
