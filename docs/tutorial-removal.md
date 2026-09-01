# Deleting the first-launch tutorial

_[#75](https://github.com/emmanuel-h/Todolist/issues/75) · landed 2026-09-01 · Phase 2.0 of
[the device-feedback plan](device-feedback-plan.md)_

The six-scene phantom-hand tour is gone. 5 338 lines of Kotlin across 24 main and 20 test
files, plus the unpicking of 18 more that referenced it.

## Why

The tour existed because every act on a row was a gesture, and a gesture has to be
demonstrated before it can be found. That premise is being removed by the three tickets
after this one: [#72](https://github.com/emmanuel-h/Todolist/issues/72) puts real controls
on the row, [#67](https://github.com/emmanuel-h/Todolist/issues/67) puts a visible route to
a date on a list, and [#69](https://github.com/emmanuel-h/Todolist/issues/69) pins the add
line where it can be found. Once an affordance is simply visible, 28 seconds of somebody
else driving the reader's app buys nothing.

It went **first** rather than last for two reasons. The tour's final scene mimes both
swipes, so landing it after #72 would have meant rewriting that scene only to delete it a
phase later. And every diff after this one is smaller for the tour not being in it.

[#70](https://github.com/emmanuel-h/Todolist/issues/70), "announce the tour before it
starts", was closed as superseded: a tour that does not exist needs no warning.

## What went

| Layer | Gone |
|---|---|
| `domain/` | `TutorialAction`, `TutorialAnchor`, `TutorialScreen`, `TutorialScript`, `TutorialStep`, `TutorialStateRepository`, and the five use cases (`ShouldRun`, `Start`, `Finish`, `CleanupAbandoned`, `SaveDemoListId`) |
| `data/` | `SharedPreferencesTutorialStateRepository` |
| `presentation/` | `TutorialDirector`, `TutorialViewModel`, `TutorialOverlay`, `TutorialStage`, `TutorialUiState`, `TutorialPace`, `TutorialDemoWords` |
| `ui/tutorial/` | the whole package — anchors, overlay state, overlay UI, controller, `NonShiftingTodoListRepository` |
| `ui/` | `ListsStage`, `ItemsStage`, `PaperMotion.handGlide`, the `?` replay glyph, `ic_help.xml`, `ic_close.xml` |
| `res/` | eleven `tutorial_*` / `replay_tutorial` strings, in `values/` **and** `values-fr/` |

The demo's own bookkeeping went with it: nothing writes a "seen" flag or a demo list id to
`SharedPreferences` any more. The Auto Backup whitelist still names the database alone, but
now for one reason rather than two — a spent notification permission must not travel to a
restored device.

## What the deletion cost the surviving code

`NavStage` was two things wearing one name: the back stack the reader navigates, and the
stage the demo's hand stood on. Only the first survives — `open`, `leave`, `onItems`,
`animationsEnabled` — and `PageStage`, `TopOfStackAnchors`, `attach`/`detach`, `perform`,
`boundsOf`, `abandon` and `awaitDemoListId` all went with the second. `ListsStage` and
`ItemsStage` had no method that a finger ever reached, so both files went whole.

Both screen states stop being `TutorialAnchorHost`s, which takes `Modifier.tutorialAnchor`
off eighteen call sites and takes the per-frame layout callback off every anchored row —
the measurement that was already gated on `recordingAnchors` is now not written at all.

Four parameters existed only to carry an anchor into a component and are gone with it:
`InkAddLine.fieldModifier` and `commitModifier`, `TodoRow.toggleModifier`, and
`DateMarks.targetModifier` / `dueModifier`. `SwipeRow.staged` and
`TodoListsScreenState.demoPull` — the demo's way of holding a row aside with no finger on
it — went the same way, and a row's pull is once again read from the finger alone.

## What had to survive

- **`DateKindCaption` and the `date_kind_*_caption` strings.** The pill was written for the
  tour but the calendar sheet raises it on its own, and `SPEC.md` requires the 📅/⏰
  distinction be taught in words wherever it is met. Three wordless iterations failed at it
  before two translated strings succeeded; that evidence outlives the tour that produced it.
- **`ReminderSlip`.** First drawn inside the overlay as a promise of what circling a day
  would do. It had already been lifted out so the app could raise it in earnest, which is
  the only reason it survived the package it was born in.
- **A genuinely empty first page.** What greets a first-time reader now is the add line's
  breathing hint and the sticky pad. Whether that is enough on its own is exactly the
  question [#69](https://github.com/emmanuel-h/Todolist/issues/69) answers.

## The guard on words

`IconOnlyUiTest` pins which words each screen draws. Two things changed in it, both
tightenings: the `TutorialWordsTest` class that pinned the six narration lines and the three
demo words is gone with the strings, and the empty lists page now asserts **one** affordance
where it asserted two. It was not loosened, and no assertion in it would now pass for a
string nobody chose.

## Gates

`testDebugUnitTest` 1 089 tests green; JaCoCo 100 % line and branch on `domain/` and
`presentation/` (every missed line in `data/` is Room-generated `_Impl` code, excluded from
the mutation gate by name); Pitest 196/196 mutants killed, test strength 100 %;
`:app:lintDebug` 0 errors.

Most of this phase is `ui/`, which is outside pitest's `--targetClasses`, so it was also
driven on a Nokia X30 5G from a **fresh install** — the one launch where the tour would have
played. The page came up bare, with the masthead alone above the head rule and nothing
opposite it, and create → open → add → tick ran clean.

## Caveat

Between this and #72/#67/#69 landing, the app has neither a tour nor the controls that
replace it. **Do not cut a Play Store release in that window.**
