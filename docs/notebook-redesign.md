# The notebook redesign: two hands, an agenda, and pages you can turn

## What it does
The app read as a draft — one hand for everything, a faint masthead, a column of glyphs
louder than the names they belonged to, and a lone `2` between sections. This pass keeps the
ink-on-paper identity and finishes it:

- **Two hands.** The reader's words stay handwritten; everything the notebook says is set in
  print (Spectral). See **Two hands** in [SPEC.md](SPEC.md).
- **A cover** heads the page of lists: the day, the name, and one standing line
  (`6 lists on the go · 1 for today · 1 overdue`).
- **An agenda.** Unfinished lists are filed under Overdue / Today / Next 7 days / Later /
  No date, ordered by day within each; finished lists fold away under Finished.
- **Progress at a glance.** Each row and each list's head carries an ink line over a pencil one
  with the fraction beside it.
- **One control per list row.** The pencil, calendar and bin become a single ⋯ that holds out a
  worded sheet: Rename or colour, Give it a day / Change its day, Delete list.
- **Pages you can turn.** On a list's page a sideways swipe turns to the next or previous list;
  page dots show where you are.
- **Worded section headings** (`DONE  2`, foldable) replace the bare tally between sections on
  both pages, and the slips' questions, buttons and calendar are printed.

## Architecture
- **Domain** — `ListHorizon` and `ListHorizon.of(day, today)`; `TodoListSummary.horizon`
  stamped by `GetTodoListsWithStatusUseCase` from whichever date the list carries. 100% line,
  branch and mutation coverage.
- **Presentation** — `TodoListsViewModel.buildState` orders unfinished lists by horizon then
  day (stable, so undated lists keep their dragged order); `TodoListsState.Content.sections`
  cuts that run into `ListSection`s. No date arithmetic reaches a composable.
- **UI** — new primitives `PaperType.print` + printed styles, `InkProgress`, `SectionHeading`,
  `PageDots`, `PaperDialog(alignment)`; new screens pieces `ListsCover`, `ListActionsSheet`,
  `ListProgress`; `ItemsPager` in `PageStack`.

## Files
- `domain/ListHorizon.kt`, `domain/TodoListSummary.kt`, `domain/GetTodoListsWithStatusUseCase.kt`
- `presentation/TodoListsState.kt`, `presentation/TodoListsViewModel.kt`
- `ui/paper/PaperType.kt`, `InkProgress.kt`, `SectionHeading.kt`, `PageDots.kt`, `PaperDialog.kt`,
  `DeleteConfirmDialog.kt`, `PaperCalendar.kt`, `ReminderClock.kt`, `PaperSlip.kt`, `InkIcon.kt`
- `ui/listmeta/ListProgress.kt` (replaces `OpenCount.kt`)
- `ui/todolists/ListsCover.kt`, `ListActionsSheet.kt`, `TodoListRow.kt`, `TodoListsScreen.kt`,
  `TodoListsScreenState.kt`
- `ui/todolist/TodoListScreen.kt`, `TodoListScreenState.kt`
- `ui/nav/PageStack.kt` — `ItemsPager`, `pageOrder`, `rebind`
- `res/font/spectral_*.ttf` (subset), `res/drawable/ic_more.xml`, `ic_chevron_down.xml`,
  `docs/fonts-OFL-spectral.txt`
- Deleted: `ui/paper/SectionSkip.kt`, `ui/listmeta/OpenCount.kt` (and their baseline-profile rules)
- Tests: `ListHorizonTest`, `GetTodoListsWithStatusUseCaseHorizonTest`,
  `TodoListsViewModelSectionsTest`, `PageBindingTest`, `ListPagingTest` (drives the real
  activity and swipes), plus the updated `IconOnlyUiTest`, `TodoListRowTest`,
  `TodoListsScreenTest`, `TodoListScreenTest`, `InkGlyphFamilyTest`

## Invariants & contracts
- **Who speaks decides the hand.** Never set the reader's words in print or the app's in hand.
- **Only undated lists are dragged.** Dated sections are ordered by day; the reorder still hands
  the repository only the ids it shows, which the repository already accepts.
- **The page binding is frozen while turning.** `rebind` keeps bound lists in place, drops the
  gone and appends the new, so finishing or dating a list never turns the page under the finger.
- **Every printed word stays visible to `IconOnlyUiTest`.** New semantics merge descendants
  rather than clearing them, so the guard sees each word and pins it.
- **The ⋯ glyph belongs to the family**: one stroked path on the 24 grid with the 2-unit nib.

## Not done here
- The store screenshots in `store-assets/` still show the old design; reshoot them with the
  release skill before the next Play release.
- The baseline profile was pruned of the two deleted classes but not regenerated; regenerate it
  on a device (`./gradlew :app:generateBaselineProfile`) so the new composables are covered.
