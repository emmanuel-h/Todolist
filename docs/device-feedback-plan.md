# Device feedback — the tickets, and the order to fix them

**Status: Phases 2, 3 and 4.1 complete. Phase 4.2 (#71, colour per list) is all that is left.** Written 2026-09-01. This is a working plan, not
a feature doc — it is meant to be read after a context clear, so it says where
the work stands as well as what the work is.

## How to resume

1. Read this file top to bottom, then the **Progress** checklist below.
2. Read `SPEC.md` before touching anything, as every agent must.
3. `gh issue list --state open` — an issue still open is a phase still to do.
4. Pick up at the first unchecked box. Commit and push at every phase boundary.

## Where these came from

The reporter wrote nine items into a list inside the app itself, on a Nokia X30
5G, and asked for one ticket each. They are filed as
[#66](https://github.com/emmanuel-h/Todolist/issues/66)–[#74](https://github.com/emmanuel-h/Todolist/issues/74);
[#75](https://github.com/emmanuel-h/Todolist/issues/75) was added during the
design pass and [#70](https://github.com/emmanuel-h/Todolist/issues/70) closed
into it.
Each issue body carries the reporter's own words, the code that produces the
behaviour, and the questions the fix has to answer. **The issues are the
specification; this file is only the ordering.**

| # | Ticket | Where it lives today |
|---|---|---|
| [#66](https://github.com/emmanuel-h/Todolist/issues/66) | Ask before a delete | `ui/DeletionState.kt` — a 9 s undo slip, and leaving the page *commits* |
| [#67](https://github.com/emmanuel-h/Todolist/issues/67) | Date route not discoverable | `DateMarks` is drawn in three places, none of them a committed row |
| [#68](https://github.com/emmanuel-h/Todolist/issues/68) | Wrapped line drops a full rule | `PaperType.lineBoxOf` — leading is exactly one page pitch |
| [#69](https://github.com/emmanuel-h/Todolist/issues/69) | The `…` is too faint | `add_line_hint` in `InkTone.Margin`, breathes only on an empty page |
| ~~[#70](https://github.com/emmanuel-h/Todolist/issues/70)~~ | ~~Announce the tour~~ | **closed** — superseded by #75 |
| [#75](https://github.com/emmanuel-h/Todolist/issues/75) | Delete the tutorial | 5 338 lines across 24 main + 20 test files |
| [#71](https://github.com/emmanuel-h/Todolist/issues/71) | Add colour | `PaperPalette` — colour is a *semantic* channel (red = lost, amber = today) |
| [#72](https://github.com/emmanuel-h/Todolist/issues/72) | Buttons instead of swipe | `SwipeRow` + `CornerMark` at `REST_INK = 0.6f` |
| [#73](https://github.com/emmanuel-h/Todolist/issues/73) | Logo | `ic_launcher_foreground.xml` — a 48-unit rounded rect in a 108 viewport |
| [#74](https://github.com/emmanuel-h/Todolist/issues/74) | Notification hour | `DailyCheckSchedule.CHECK_TIME = 08:00`, and the `KEEP` policy |

## Why not in the order they were filed

**#72 is the keystone.** It changes the anatomy of a row, and #66, #67 and #69
are each a version of the same question — *where on the row does this act live* —
which cannot be answered until #72 is. Fixing them in filed order redraws the row
four times.

**#68 was to come before #72** because it settles the page pitch, and every later
row measurement is taken from it. **That turned out to be backwards and the two
were swapped on 2026-09-02.** Two findings from driving 48dp on the phone: the
complaint is not the size of the gap but that an item's own two lines are spaced
*exactly* like two separate items, so shrinking the pitch uniformly cannot answer
it — the leading has to come off the row spacing; and the thing making row heights
hard to reason about was `SwipeRow` + `CornerMark`, which #72 deletes. The pitch is
easier to settle on a row that is only `ring · title · [pencil] [bin]`.

**#75 goes first, and it deletes #70.** The tour existed because every act on a
row was a gesture that had to be demonstrated. Once #72 puts real controls on the
row there is nothing left to demonstrate, so the tour is deleted outright rather
than announced — #70 is closed as superseded. Deleting it *before* #72 means #72
never has to rewrite the tour's last scene, which mimes both swipes, only for the
scene to be deleted a phase later.

**Do not cut a Play Store release between #75 landing and #72/#67/#69 landing** —
in that window the app has neither a tour nor the controls that replace it.

**#73 is free.** Pure asset work, no code coupling — it can run at any point,
including in parallel with everything else.

**#74 before #71** because #74 creates the app's first settings surface and #71
probably wants a home in it. That decision should be made once.

## Progress

- [x] **Phase 1 — Design pass** — agreed 2026-09-01, drawn below
- [x] **Phase 2.0 — #75** delete the first-launch tutorial — landed 2026-09-01, [doc](tutorial-removal.md)
- [x] **Phase 2.1 — #68** the line and the row become two measures — landed 2026-09-02, [doc](line-and-row.md). Not the 48dp the design pass chose: 48dp was built, driven and reverted because it keeps the ratio that is the actual complaint
- [x] **Phase 2.2 — #72** row controls replace the swipe — landed 2026-09-02, [doc](row-controls.md)
- [x] **Phase 2.4 — #66** confirm a delete — [doc](confirm-before-delete.md)
- [x] **Phase 2.5 — #67 + #69** the two discoverability tickets — [doc](discoverability.md)
- [x] **Phase 3 — #73** launcher icon — the sheet comes off the tile, [doc](app-icons.md)
- [x] **Phase 4.1 — #74** notification hour + first settings surface — [doc](reminder-hour.md)
- [ ] **Phase 4.2 — #71** colour per list

---

## Phase 1 — Design pass — **agreed 2026-09-01**

Everything below was drawn, reacted to and agreed before any code. It is the
design #75, #68, #72, #66, #67 and #69 implement. Where the code and this section
disagree, this section is what was agreed and the code is wrong.

Glyph key: `( )` ring open · `(v)` ring ticked · `[/]` pencil, edit ·
`[X]` bin, delete · `[#]` calendar, give it a day · `[\]` pen, the writing line

### Screen 2 — the items page

```
  BEFORE                                AFTER
  +----------------------------------+  +----------------------------------+
  | <-   Groceries                   |  | <-   Groceries                   |
  |==================================|  |==================================|
  |   v >                     < X    |  |  ( )  Buy bread          [/] [X] |
  |  ( )  Buy bread                  |  |  ( )  Milk               [/] [X] |
  |                                  |  |  (v)  Eggs               [/] [X] |
  |   v >                     < X    |  |                                  |
  |  ( )  Milk                       |  |                                  |
  |                                  |  |                                  |
  |   v >                     < X    |  |- - - - - - - - - - - - - - - - - |
  |  (v)  Eggs                       |  |  [\]  Add an item                |
  +----------------------------------+  +----------------------------------+

     18 half-glyphs at 0.6 ink,            two marks per row, full ink, on
     drawn a rule above the words          the rule they belong to; the add
     they belong to; the ... add           line pinned and always on screen
     line is below the fold
```

Carried over unchanged: the ring's tap ticks the item; the title's tap edits it
in place — the pencil is the discoverable route to the same act and the title tap
stays as the fast one; long-press-and-drag still reorders, which is a different
gesture and is not part of what is being removed.

The 52dp `CORNER_ROOM` spacer at the row's end becomes the two controls, so the
title column loses about 44dp.

### Screen 1 — the lists page

```
  BEFORE                                AFTER
  +----------------------------------+  +----------------------------------+
  |         To do list           (?) |  |         To do list               |
  |==================================|  |==================================|
  |   / >                     < X    |  |  Groceries        3 [/] [#] [X]  |
  |  Groceries                    3  |  |  Errands   5 Sep  7 [/]     [X]  |
  |                                  |  |  Repairs   2 Sep  1 [/]     [X]  |
  |   / >                     < X    |  |                                  |
  |  Errands            5 Sep     7  |  |                                  |
  +----------------------------------+  +----------------------------------+

     (?) replay glyph, top right          (?) gone with the tutorial (#75)
```

**The date slot holds either the calendar button or the date itself** — this is
the whole of #67 and it costs no extra width:

- **no date** → `[#]`; pressing it opens the calendar
- **has a date** → the jot, which is *already* pressable today
  (`DateJot.onRewrite`) and already opens the same calendar

No row ever carries four controls. A list may hold a target date **or** a due
date and never both, so the slot is never contested.

Consequence: a dateless row's name column loses about 92dp — roughly 250dp to
160dp on a 360dp screen — before it starts eliding.

### A control at rest and pressed

```
  at rest          pressed             what changes
  -----------      -----------         --------------------------
     [/]              [/]              ink  Margin -> Ink
                     (  )              PaperFocusMark, as the
                                       title already uses
                                       haptics.pickUp()
```

Drawn at full ink, not 0.6 — these are controls now, not a hint that a gesture
exists.

### #68 — the page pitch

```
  TODAY  pitch 56dp                   AGREED  pitch 48dp
  _______________________________     _______________________________
   ( ) Multiline items should          ( ) Multiline items should


  _______________________________     _______________________________
   have a smaller interline             have a smaller interline


  _______________________________     _______________________________
   ( ) Add colors                       ( ) Add colors

  leading 56/18 = 3.1x                leading 48/18 = 2.7x
```

The pitch is not derived from the tap target — it is the literal
`PaperType.base.lineHeight = 56.sp`, and `pagePitch()` only floors the result at
`TOUCH_FLOOR = 48.dp`. So 48dp is reachable by changing one constant, with every
row still exactly one rule tall and still a legal tap target.

This is the conservative half of the fork. The other was **34dp with a row
min-height of 48dp** — a 1.9x leading, needing `RuledRow`, `RuledPage` and
`seatOnRule` to stop treating row height and pitch as one number. It is still
available. **Drive 48dp on the phone before calling 2.1 done**; if it does not
answer the complaint, 34dp is the next move.

### #69 — the pinned add line

The complaint understates it: `InkAddLine` is a `LazyColumn` item after the last
row (`TodoListScreen.kt:318`), so on a page that fills the screen it is not faint
but **off-screen**. The reporter's own nine-item list only showed the `…` after
scrolling.

```
     ink hierarchy on the pinned line
     ------------------------------------
     ( )  Rework the logo        <- Ink, full
     ------------------------------------
     [\]  Add an item            <- pen: Ink, full
          ^^^^^^^^^^^            <- word: Margin, faint
```

Pinned above the bottom inset, always on screen. The pen carries the visibility;
the word stays in `InkTone.Margin` so it does not read as a real item — which is
what `GhostHint` already does, so only the pen and the pinning are new.

New strings in `values/` **and** `values-fr/`; `IconOnlyUiTest` updated on
purpose, never weakened.

### #66 — the delete confirmation

```
  press [X] on "Groceries"

    +-------------------------+
    |  Delete "Groceries"?    |
    |  and the 9 items on it  |
    |                         |
    |    Cancel      Delete   |
    +-------------------------+

    Delete -> written at once
    Cancel -> nothing happened
```

**The confirmation replaces the undo slip**, on both screens. One decision point,
and nothing commits behind the reader's back.

What goes: `ui/DeletionState.kt`, `UNDO_SLIP_MILLIS`, and the rule that leaving
the page commits a pending delete — which is the surprise the reporter hit.

What may stay: the `TearOff` animation. A confirmed delete can still tear the row
off the page; it just has nothing to wait for afterwards.

Free simplification: reorder currently has to reconcile two index spaces because
"the page hides a torn-off row for the length of its undo slip while the
repository still holds it". With no slip, that disagreement stops existing.

`delete_list` and `cancel` already exist unused in `strings.xml`. The prompt
needs words; `IconOnlyUiTest` is updated on purpose.

## Phase 2 — The page's anatomy

### 2.0 — #75 delete the first-launch tutorial

First, because it shrinks every diff after it. See the issue for what must
survive the deletion — chiefly `DateKindCaption` and the `date_kind_*_caption`
strings, which the calendar sheet uses on its own.

### 2.1 — #68 the line and the row become two measures — **done, but not as designed**

The design pass chose 48dp, "the conservative half of a fork", reachable by one constant.
**It was built, driven on the phone and reverted.** Shrinking the pitch uniformly leaves the
ratio untouched, and the ratio is the complaint: an item's own two lines were spaced exactly
like two separate items.

What landed instead splits the one number in two — line pitch 28dp, row = written lines plus
one blank rule — so an item's lines sit 28dp apart and the next item 56dp away. See
[line-and-row.md](line-and-row.md) for the font-scale sweep behind 28 and for the touch-target
trap that comes with a rule smaller than a finger.

It also landed **after** #72 rather than before; see "Why not in the order they were filed".

### 2.2 — #72 row controls replace the swipe

The keystone. The tour that mimed both swipes is already gone by this point
(2.0), so this phase only has to build the controls.

**Decided in Phase 1:** the swipe goes **entirely** — `SwipeRow.kt` and
`CornerMark.kt` are both deleted, and there is one way to do each thing. The
controls are **persistent glyphs on every row**, at full ink, drawn on the rule
they belong to: `[pencil] [bin]` on an item, `[pencil] [calendar] [bin]` on a
list. The 52dp `CORNER_ROOM` spacer at each row's end becomes the controls.

Carried over unchanged: the ring's tap (tick), the title's tap (edit in place —
the pencil is the discoverable route to the same act, the tap stays the fast
one), and long-press-and-drag to reorder, which is a different gesture and is not
part of what is being removed.

Keep: the `RowVerbs` custom accessibility actions for reorder (`move_up` /
`move_down`) — real buttons make some verbs redundant but not those.

### 2.4 — #66 confirm a delete

**Decided in Phase 1:** the confirmation **replaces** the undo slip, on both
screens — see the drawing above. `ui/DeletionState.kt` and `UNDO_SLIP_MILLIS`
go; `TearOff` may stay as the confirmation's answer.

### 2.5 — #67 + #69 discoverability

Both collapse to "put a real mark on the row / on the add line" once 2.2 exists,
and both are drawn above: #67 is the date slot on the list row, #69 is the pinned
add line with a pen at full ink and a word in `InkTone.Margin`. Words go in
`values/` **and** `values-fr/`, and `IconOnlyUiTest` is updated deliberately —
never weakened.

## Phase 3 — #73 launcher icon

Independent of everything; can run in parallel from the start. Three drawables
must stay consistent — `ic_launcher_foreground`, `ic_launcher_background`,
`ic_launcher_monochrome` — and the mark must read under circle, squircle, rounded
square and the themed monochrome mask. Check the Play Store listing assets too.

## Phase 4 — New surfaces

### 4.1 — #74 notification hour

Creates the app's first settings surface. Two traps: `ExistingPeriodicWorkPolicy
.KEEP` must become `UPDATE` or a changed hour silently never takes effect; and
`LocalTime` under `minSdk 24` is desugared, so `:app:lintDebug` must stay clean
(`NewApi` is fatal).

### 4.2 — #71 colour per list

Largest by some margin: domain field, `TodoListEntity` column, Room migration,
picker. Lands on both the settled row shape from 2.2 and the settled settings
surface from 4.1. Colour the reader chooses must not collide with the semantic
channel — see the open question below.

---

## Open question

Only one is left; it is not needed until Phase 4.2.

**#71 — reader colour on the paper, never on the ink?** That is the only way
found so far to keep a list the reader painted red distinguishable from a list
that is overdue in red.

## Gate reminders

- `ui/` is outside pitest's `--targetClasses`. Phases 2.1, 2.2, 2.5 and 3 are
  almost entirely `ui/` — **the gate passing is not evidence they were tested.**
  Drive them on a device.
- Phases 2.0, 2.4, 4.1 and 4.2 touch `domain/`, `data/` or `presentation/` and do
  face the full 100 % line + branch + mutation gate. 2.0 is a deletion, so the
  gate there means the *survivors* still measure 100 %.
- `./gradlew testDebugUnitTest createDebugUnitTestCoverageReport pitest` and
  `./gradlew :app:lintDebug`.
- Agent order for anything user-facing: `developer`, then `ui`.
