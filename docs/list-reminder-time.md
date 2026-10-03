# A reminder time for each list

_[#93](https://github.com/emmanuel-h/Todolist/issues/93)_

> We should be able to specify a specific hour for each list … 7AM to get the things to grab
> before going, 12PM to not forget to do stuff before going to the gym.

The app-wide hour ([#74](reminder-hour.md)) could not serve every list. A list may now carry its
own time; one without keeps following the app-wide hour, so nothing changes for a reader who
never sets one.

```
  circle a day on the calendar sheet → the ink clock opens for that list
  list's page head line:   ‹  Before the gym   ⏰ 22/09 12:00   (own time, ink)
                           ‹  Groceries        📅 23/09  8:00   (app-wide, faded)
```

## One check per distinct time

- `daily_notification_check` is unchanged in name and input (none), and now serves only lists
  with no time of their own. Work armed before this change therefore keeps its meaning.
- Each distinct own time gets `daily_notification_check@<minute of day>`, with the minute in
  its input data, a shared tag, and a per-minute tag. `WorkInfo` does not expose input data, so
  the per-minute tag is how a stale check is recognised.
- `NotificationScheduler.ensureDailyChecks(ownTimes)` is the reconcile: `KEEP` for the app-wide
  check and every own time, then cancel tagged checks for times no list has. It is what launch
  calls (via `SyncDailyChecksUseCase`, on `writeScope` — it reads the database) and what a
  changed list time calls. It never cancels a pending run for a time still in use.
- `rescheduleDailyCheck(slot)` (`UPDATE` + `setNextScheduleTimeOverride`) is for a changed
  app-wide hour and for a check re-aiming itself after its run.
- `DailyNotificationWorker.execute(slot)` posts only the lists whose `reminderSlot` is that slot.

## Shape

- `domain/ReminderSlot` — `AppWide | At(time)`; `TodoList.reminderSlot` derives it.
- `domain/SetListReminderTimeUseCase` — minute of day in `0..1439`, or null to rub out.
- `domain/SyncDailyChecksUseCase` — lists → distinct own times → `ensureDailyChecks`.
- `data/` — `reminderMinute INTEGER` (nullable) on `todo_lists`, schema 9, `MIGRATION_8_9`; a
  single-column DAO update, so rewriting a list's name/date/colour never touches the time.
- `presentation/TodoListsViewModel.setListReminderTime` — writes, then reconciles.
- `ui/paper/ReminderClock.kt` — the clock, lifted out of the settings slip, with an optional
  rub-out (`rub_out_time`, pinned in `IconOnlyUiTest`).

## Found on the way

The first cut stored `toSecondOfDay() * 60` for a column of minutes. It round-tripped through
its own read, so a save-and-reload test would never have noticed; a test that reads a known
minute (720 → 12:00) and one that writes a non-round time (07:30 → 450) do.
