# List Finished — the tick, the shower and the settle

## What it does
When the tick that empties a list is written, the page celebrates:
[#45](https://github.com/emmanuel-h/Todolist/issues/45). A check the size of the sheet
strokes itself across the paper, a shower of punched-out chads and torn scraps is thrown
up from where the reader's finger was, and the page lifts and settles under both. About a
second and a half, then it is gone and the list is simply finished.

It fires on the **transition only**. Opening a list that was already finished says nothing
— there was no tick.

## Architecture
- **Layers**: `domain` (a new event), `presentation` (deciding when it happened), `ui` (drawing it)
- **Key types**: `AnimationEvent.ListCompleted(lastItemId)`; `FinishFlourish` +
  `PaperFinish` in `ui/paper/`
- **Async contract**: `TodoListViewModel.animationEvents` — a `SharedFlow` that existed
  since the View days and had no consumer left after the Compose migration. This is its
  first one.

## Files
- `app/src/main/java/fr/mandarine/todolist/domain/AnimationEvent.kt` — `ListCompleted`
- `app/src/main/java/fr/mandarine/todolist/presentation/TodoListViewModel.kt` — `toggleEvents`
  and `nothingLeftActive`; `applyAndPublishWithEvent` becomes `applyAndPublishWithEvents`
  and takes a list; the flow's buffer goes from 1 to 4
- `app/src/main/java/fr/mandarine/todolist/ui/paper/PaperFinish.kt` — the flourish: the
  polyline the mark is stroked along, the scrap physics, and `noteWhereTheHandWent`
- `app/src/main/java/fr/mandarine/todolist/ui/todolist/TodoListScreenState.kt` — `lastTouch`,
  `finishedOn`
- `app/src/main/java/fr/mandarine/todolist/ui/todolist/TodoListScreen.kt` — plays it, and
  buzzes whether or not it plays
- `app/src/main/java/fr/mandarine/todolist/ui/nav/PageStack.kt` — collects `ListCompleted`
- `app/src/test/java/fr/mandarine/todolist/presentation/TodoListViewModelAnimationTest.kt` — six tests
- `app/src/test/java/fr/mandarine/todolist/domain/AnimationEventTest.kt` — three tests
- `app/src/test/java/fr/mandarine/todolist/ui/paper/PaperFinishTest.kt` — the geometry

## Invariants & contracts
- **`ListCompleted` comes after `ItemCompleted`, not instead of it.** A tick is still a tick
  when it is the last one, and a future consumer of `ItemCompleted` must not silently miss
  the one that mattered most. This is why the flow's buffer is 4 and not 1: with room for
  one, the second event pushed the first out under `DROP_OLDEST`.
- **The question is asked after the write, not before it.** `nothingLeftActive()` reads the
  repository once `toggleTodoUseCase` has run, so "did that finish the list" is a fact and
  not a prediction. It is asked only inside a toggle, which is what keeps a merely-opened
  finished list quiet.
- **A restore never celebrates**, even on a list that is otherwise complete.
- **An empty list never celebrates.** `nothingLeftActive()` requires at least one item, so a
  toggle on a list with nothing in it — a stale id, a ghost row — says only `ItemCompleted`.
- **The view model does not know where the finger was**, and must not learn. The page reads
  the touch off the pointer itself (`noteWhereTheHandWent`, on `PointerEventPass.Initial`,
  consuming nothing) and hands the flourish the position. Reading it off the ticked row's
  bounds would not work anyway: by the time the event arrives that row has travelled into
  the completed section.
- **Reduced motion gets the strike and the buzz and nothing else.** `animationsEnabled` gates
  the flourish alone; `haptics.submit()` fires either way, because a reader who asked for
  stillness asked about the screen, not about being told they are done.
- The mark is stroked as two lines with round caps rather than as a `Path`. A path cannot be
  measured off a device — `android.graphics.Path` is not there in a plain JVM test — and the
  geometry is the half of this worth testing.
- The scraps are seeded from the finishing item's id, so the same finished list throws the
  same confetti twice and a recomposition cannot reshuffle the sky mid-flight.

## UI
- **Screen(s)**: the page of items
- **Design decisions**: the throw is deliberately soft — 520–1250 px/s into a wide cone
  against 2200 px/s² of gravity. A finishing tick is usually written on the last active row,
  which sits just under the head line, and scraps thrown at party-popper speed from there are
  off the top edge in three frames. The colours are palette-only (`stickyNote`,
  `stickyNoteMid`, `inkBlue`, `inkAmber`, `inkRedSoft`) so a shower of paper never turns into
  a gradient. The three beats run **together**, not in a queue: played in sequence the whole
  thing ran for three seconds, and a mark that sits on the page waiting for its confetti stops
  reading as a flourish and starts reading as something to dismiss.
