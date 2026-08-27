# Add Line — the tick that commits the line

## What it does
Puts a tick at the end of the rule being written on, so a line can be finished by
pressing something on the page. Until now the only way to commit an inline add line —
a new list on the page of lists, a new item on the page of items — was the keyboard's
own Done key. The page said the line could be *abandoned* (the sticky pad turns to a
`−` while a line is open) and never that it could be finished, which is what
[#47](https://github.com/emmanuel-h/Todolist/issues/47) reported.

The tick is not on the rule until there is something to commit. An always-present tick
over an empty line is a control that does nothing, and a reader learns to stop looking
at it.

## Architecture
- **Layers**: ui only — no domain, data or ViewModel change. Committing runs the same
  lambda the IME's Done key runs.
- **Key types**: `InkAddLine` gains `commitSpoken: String` (required) and
  `commitModifier: Modifier`; a private `RowScope.CommitMark` renders the glyph.
  `GlyphFoot.check` is new.
- **Async contract**: none

## Files
- `app/src/main/java/fr/mandarine/todolist/ui/paper/InkAddLine.kt` — the shared commit
  lambda, the `CommitMark` composable, and the two new parameters
- `app/src/main/java/fr/mandarine/todolist/ui/paper/InkIcon.kt` — `GlyphFoot.check`
  (`18/24`: the tick's path bottoms out at y=17 and the round cap adds half a nib under it)
- `app/src/main/java/fr/mandarine/todolist/ui/todolists/TodoListsScreen.kt` — passes
  `commit_list`, and moves the `SubmitListButton` tutorial anchor onto the tick
- `app/src/main/java/fr/mandarine/todolist/ui/todolist/TodoListScreen.kt` — passes
  `commit_item`, and moves the `SubmitItemButton` tutorial anchor onto the tick
- `app/src/main/res/values/strings.xml`, `values-fr/strings.xml` — `commit_list`,
  `commit_item` (content descriptions; the glyph draws no text)
- `app/src/test/java/fr/mandarine/todolist/ui/paper/PaperPrimitivesTest.kt` — three tests:
  absent on an empty line, absent on a line holding only blank space, and commits what is
  written when tapped

## Invariants & contracts
- **The tick and the IME's Done are one act.** Both call the same `commit` lambda:
  `onCommit(text)` when the text is not blank, `haptics.submit()`, then the caret back on
  a fresh line. Do not let the two paths drift; a reader who commits one way and then the
  other must not be able to tell which they used.
- **Blank never commits.** `isNotBlank()` gates both the glyph's presence and the commit
  itself, so a line of spaces neither shows a tick nor can be finished by one.
- The glyph arrives by widening the rule (`expandHorizontally`) rather than fading in over
  it, so the writing slot gives up the room instead of the tick landing on top of what is
  being written. Under reduced motion it is a plain fade.
- **The tutorial's submit anchors now point at something real.** `SubmitListButton` and
  `SubmitItemButton` used to be hung on the whole add-line row because no submit control
  existed; the demo mimed a tap on a rectangle with nothing in it. They are on the tick.
  Anything that removes the tick must move those anchors, or the hand fades off the page
  (→ see `first-launch-tutorial.md`).

## UI
- **Screen(s)**: both — the page of lists and the page of items share `InkAddLine`
- **Layout file(s)**: none; the app has no `res/layout/`
- **Design decisions**: the tick is `InkTone.Acted`, the same blue as the live caret and
  the wet tick on a row — it is the ink the reader's own action is written in. It is seated
  on the rule (`IconSeat.OnRule`) rather than centred in its box, so it sits on the line
  the text sits on rather than floating beside it.
