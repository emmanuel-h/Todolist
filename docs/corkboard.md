# The corkboard: lists pinned up, days on demand

Direction 4 of the 2026-10 redesign canvas — the corkboard of direction 3 with the date
features of direction 1, kept secondary because most lists have no day.
Issues [#118](https://github.com/emmanuel-h/Todolist/issues/118),
[#119](https://github.com/emmanuel-h/Todolist/issues/119),
[#120](https://github.com/emmanuel-h/Todolist/issues/120).

## What it does
- **The page of lists is a cork board.** Each list is a card of paper taped to it in the
  reader's colour, with its name, one printed status line, and the first three things still to
  do (`+ N more` for the rest). Cards lie two to a row on a phone.
- **Days step back.** The board keeps the reader's own order; the horizon headings are gone.
  A dated card carries a page torn off a calendar in its corner, and its status line says
  `2 DAYS LATE`, `TODAY` or the day. A slip under the head names what is due this week, and
  only appears when something is.
- **A bar at the foot: Lists | Calendar.** No Today tab. The Calendar tab is a month sheet
  marking the days lists fall on, the dated lists as slips in agenda order, and a closing
  label saying the undated ones stay in Lists.
- **The opened list is the card, lifted** off the dimmed cork, taped across its top. A list
  with no day offers one: an **Add a date** chip where the jot would be.
- **Any unfinished card can be dragged anywhere** on the board, in two directions.

## Architecture
- **Domain/data** — `ListPreview` and `TodoRepository.previewsByList(limit)`: one
  correlated-subquery read of the first open items of every list (SQLite 3.9 has no window
  functions). `TodoListSummary` gains `preview` and `daysLate`.
- **Presentation** — `TodoListsState.Content.activeSummaries` keep repository order; `agenda`,
  `thisWeek` and `undatedCount` are derived on `Content`. `sections`/`ListSection` are gone.
- **UI**
  - `ui/paper/CorkBoard.kt` — the baked cork tile, `CorkSurface`, `pinnedPaper`, `PaperTape`,
    `TearOffDay`, `cardTilt`, `CorkDimens`; palette fields `cork`, `corkDim`,
    `corkFleckDark`, `corkFleckLight`, `tape` (light and night).
  - `ui/todolists/ListCard.kt` replaces `TodoListRow.kt`; `ThisWeekStrip.kt`; `BoardTabs.kt`
    (`BoardTabBar`, `BoardBarHeight`); `TodoListsScreen.kt` hosts the board and the calendar.
    `TodoListsScreenState` saves `tab` and `calendarMonth`.
  - `ui/calendar/CalendarPage.kt` — the Calendar tab.
  - `ui/reorder/BoardReorder.kt` — `BoardDrag` follows the finger in window coordinates over
    bounds each card reports; `nextBoardOrder` + `SwapGuard` keep a tall card and a short one
    from flickering.
  - `ui/todolist/TodoListScreen.kt` — the sheet on `CorkSurface(dimmed = true)` with
    `pinnedPaper(lifted = true)` and tape; `AddDateChip`.

## Invariants
- No blue on the resting board: an upcoming day is pencil, a missed due day red, today amber.
- The reader's colour is tape, never ink.
- New words are in `values/` and `values-fr/` and pinned in `IconOnlyUiTest`.

## Follow-ups
- The committed baseline profile lost its `TodoListRowKt` rules; regenerate it on a device
  (`./gradlew :app:generateBaselineProfile`) so the card and the board are profiled.
