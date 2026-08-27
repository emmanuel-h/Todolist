# Tutorial Rework — names, dots, words and time

## What it does
Closes [#48](https://github.com/emmanuel-h/Todolist/issues/48): *"Let's check what step need to
be shown, which should be shown longer than others. Let's add wording in it also."* Four
changes, in that order.

**1. The steps are named for what they do.** Every step after the first was named for the
scene *before* it — `SET_DUE_DATE` opened a list, `OPEN_LIST` wrote items into it,
`DELETE_LIST` also opened and closed an edit sheet. They are now `A_DAY_AND_A_NOTE`,
`OPEN_IT`, `WRITE_ITEMS`, `TICK_AND_MOVE`, `EDIT_AND_TEAR`.

**2. The opening scene gets a dot of its own.** There were six scenes and five dots: the
opening — pad taken, name written, day circled, line committed, the longest and busiest scene
in the tour — shared the first dot with the beat after it, so the pill did not move until a
third of the tour was over. Six scenes, six dots.

**3. Every scene says what it is doing**, in one translated line on a paper slip pinned
top-centre under the status bar, up for the whole of the scene. **The demo's own words are
translated too** — the list it writes and the two items it puts on it were Kotlin literals in
`TutorialDirector`, so a French reader watched a hand write "🛒 Groceries" onto a page whose
every other word had been translated. `presentation/` cannot read a resource, so they are
handed down from the composition root as a `TutorialDemoWords`.

**4. The time was redistributed toward the beats nobody can guess.** The pacing was inverted:
the long-press drag — the one gesture no reader discovers unaided — held its grip for 100 ms
before the row moved and got 1.6 s in total, while obvious beats got more. The grip is now
held for 400 ms, the moved row for 250, and both swipes in the last scene hold for 300 rather
than 100.

## Architecture
- **Layers**: `domain` (`TutorialStep`), `presentation` (`TutorialLine`, the `TutorialOverlay`
  contract, `TutorialDirector`), `ui` (the slip, the dot count)
- **Key types**: `TutorialLine` — six values, one per scene; `TutorialOverlay.narrate(line)`
- **Async contract**: unchanged; narration is a rest-free beat like the captions

## Files
- `app/src/main/java/fr/mandarine/todolist/domain/TutorialStep.kt` — renamed, five values
- `app/src/main/java/fr/mandarine/todolist/presentation/TutorialOverlay.kt` — `TutorialLine`, `narrate`
- `app/src/main/java/fr/mandarine/todolist/presentation/TutorialDirector.kt` — `openWith`, the rebalance
- `app/src/main/java/fr/mandarine/todolist/ui/tutorial/TutorialOverlayState.kt` — `narration`,
  `narrationAlpha`, `PROGRESS_DOT_COUNT` 5→6, `filledDotsFor`, banner hold 1700→2200
- `app/src/main/java/fr/mandarine/todolist/ui/tutorial/TutorialOverlayUi.kt` — `NarrationSlip`,
  `narrationStringRes`
- `app/src/main/res/values/strings.xml`, `values-fr/strings.xml` — six lines each
- `app/src/test/java/fr/mandarine/todolist/ui/IconOnlyUiTest.kt` — `TutorialWordsTest`
- `TutorialDirectorTest`, `TutorialOverlayStateTest`, `TutorialOverlayControllerTest` — updated

## Invariants & contracts
- **Six scenes, six dots, six lines.** `TutorialWordsTest` requires `TutorialLine.entries.size
  == TutorialStep.entries.size + 1` and `TutorialOverlayStateTest` requires
  `PROGRESS_DOT_COUNT` to equal it. Adding a scene therefore fails in three places until it
  has a dot, a line and a translation.
- **The words are pinned verbatim.** `TutorialWordsTest` asserts the exact six sentences, and
  that each differs from its French. Do not weaken it into "resolves to something non-empty" —
  that is the whole point of the file it lives in.
- **A scene says its line before it acts.** Narration announced halfway through a scene has the
  reader reading a sentence about something they already watched happen.
- **The same line twice is one line.** `narrate` returns early when the line is unchanged —
  `EDIT_AND_TEAR` is narrated from two places (the return-to-lists beat and the delete beat,
  which are the same scene played on two pages) and must not flicker between them.
- **The narration steps aside for the reminder banner** rather than stacking with it: its alpha
  is multiplied by `1 - bannerProgress`. They want the same strip of paper and the banner is
  the point of the scene whose line it covers.
- **`TICK_AND_MOVE` must keep leaving one item active.** It already had to, so that the last
  scene finds the demo's first row and not the reader's; since #45 it matters twice over, as a
  demo list driven to empty would set off the finishing flourish over a page nothing but the
  tour is driving.
- **`SCRIPTED_TOUR_MILLIS` is quoted in `SPEC.md`.** When the pacing changes, change both.

## UI
- **Screen(s)**: the overlay, over both pages
- **Design decisions**: the slip is pinned rather than anchored to whatever the hand is doing.
  A line that followed its subject would have the reader's eye chasing it around the page
  instead of watching the hand — and the hand is the thing being taught. The two
  `date_kind_*_caption` pills stay anchored where they are: they point at a particular glyph,
  not at a scene.
