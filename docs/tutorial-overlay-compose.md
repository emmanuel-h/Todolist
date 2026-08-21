# Tutorial Overlay in Compose

## What it does
The phantom-hand overlay that plays the five-scene first-launch tour is now Jetpack Compose, layered over each screen inside the same `setContent` call. `overlay_tutorial.xml` was the last layout in the project; `res/layout/` is gone.

The tour itself is unchanged: hand glides, taps, grips and releases; the caption slip explains the target/due distinction; the notification banner drops in from the top; five progress dots and a ✕ sit at the bottom.

## Architecture
The 284-line view controller split in two along the seam Phase 2 established for the sticky pad — animation state apart from the composable that draws it.

| Concern | Lands in | Tested by |
| --- | --- | --- |
| Where things are, how they move | `ui/tutorial/TutorialOverlayState.kt` | plain JUnit, on a stub frame clock |
| What they look like | `ui/tutorial/TutorialOverlayUi.kt` | Robolectric + Compose |
| Which scene plays when | `ui/tutorial/TutorialOverlayController.kt` | Robolectric |

`TutorialOverlayState` *is* the `TutorialOverlay` the `TutorialDirector` drives — the same interface the view controller implemented, so `presentation/` did not move. Each suspend method animates an `Animatable` and returns when it settles.

## Files
- `ui/tutorial/TutorialOverlayState.kt` — the `Animatable`s, the snapshot state the composable reads, and the pure geometry (`handTargetFor`, `captionTopFor`, `bannerTranslationFor`, `filledDotsFor`, `handRimAlpha`)
- `ui/tutorial/TutorialOverlayUi.kt` — `TutorialOverlay`, the hand, the three paper slips, the touch guard, the banner and caption text
- `ui/tutorial/TutorialOverlayController.kt` — `handleState`, scene launching, skip
- `ui/tutorial/TutorialAnchors.kt` — unchanged from phase 4

**Deleted**: `ui/TutorialOverlayController.kt` (284), `res/layout/overlay_tutorial.xml` (137), `res/drawable/dot_tutorial.xml`, and `res/layout/` itself.

## Invariants & contracts
- **Scenes need a frame clock of their own.** They animate outside any composition, and a bare `Dispatchers.Main` carries no `MonotonicFrameClock` — the first `animateTo` throws `IllegalStateException` and takes the app down. `TutorialOverlayController` runs them on `AndroidUiDispatcher.Main`, which does. Every unit test passed with `Dispatchers.Main`, because each supplied its own clock; the device crashed on the first scene.
- **There is nothing to attach or detach.** The overlay is composed for the life of the screen and shown by `visible`. The view version added itself to the decor view and had to re-add itself on replay; that whole class of bug is gone, and the test that guarded it now asserts the behaviour instead of the plumbing.
- **The overlay swallows touches, but not from its own skip button.** It takes the gesture on `PointerEventPass.Main`, so children have already had their chance. It cannot block IME text reaching a focused field — neither could the view.
- **Anchors are screen coordinates; the overlay is not.** The hand lived in a window overlay, so `TutorialBounds` is in screen space. The overlay reports its own origin through `onGloballyPositioned` and every target is offset by it.
- **The banner reads its own height at draw time.** Its travel is `-(height + gap)` to the status-bar inset, so `graphicsLayer` computes it from `size` rather than the composable measuring itself twice. It draws transparent until measured, which is what replaces the view's 50ms wait.
- **A date picker still covers the hand.** It is a separate window, as `android.app.DatePickerDialog` was. The script does not point at anything while it is open.

## UI
- **Screen(s)**: both, as an overlay. States — opening, each of the five scenes, caption shown and swapped, banner in flight, dismissed.
- **Design decisions**:
  - **The three surfaces are paper slips, not Material cards.** Square corners, a hairline rule for an edge, `paperSheet` ground, no elevation — the same construction `PaperDialog` uses. They were the last drop shadows in the app, which the paper design says it has none of. Agreed before the work started.
  - **The hand gained a rim.** A 2dp `inkBlueDeep` ring around the blue wash, darkening as the hand shrinks into a tap, so a press reads as pressure on the page rather than a shrinking blob. Agreed before the work started.
  - **The banner and caption keep their semantics.** The view marked every overlay text `importantForAccessibility="no"`. The caption is the app's one piece of real copy — the documented exception to the icon-only rule — and the banner names a list and a date, so both are left readable. The hand and the dots are decoration and are cleared.
