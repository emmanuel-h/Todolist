# Daily Notifications

## What it does
Once a day, at the hour the reader chose (08:00 until they change it — see
[reminder-hour.md](reminder-hour.md)), the app posts one notification per list whose **due date
is today** or whose **target date is tomorrow**. Tapping one opens that list, with the page of
lists already beneath it on the back stack. On API 33+ the `POST_NOTIFICATIONS` permission is
asked for once, at the moment the first reminder of either kind is persisted; if it is refused
the app stays silent, and writing another reminder opens the system's own notification page.

## Architecture
The check is a WorkManager job. It was an `AlarmManager` chain with two `BroadcastReceiver`s
and a `RECEIVE_BOOT_COMPLETED` permission; WorkManager persists work across reboots itself, so
all of that is gone.

- **Layers**: domain (the decision), data (the platform), root (the composition)
- **Key types**:
  - `ListNotification` — sealed: `DueDateToday(list)` and `TargetDateTomorrow(list)`
  - `ListNotifier` — domain contract for posting a batch
  - `NotificationScheduler` — domain contract for arming the check. Two methods, and the
    difference between them is the whole point: `ensureDailyCheck` must not disturb a run that
    is already armed, `rescheduleDailyCheck` moves it
  - `ComputePendingNotificationsUseCase(clock)` — pure Kotlin; the whole decision
  - `DailyNotificationWorker` — reads, computes, posts, then re-aims the next run. Plain
    Kotlin, so all of it is under the quality gates
  - `AndroidListNotifier(context, opens)` — one `NotificationCompat` per list, tagged by list
    id so two lists never overwrite each other; creates the `todo_reminders` channel
  - `WorkManagerNotificationScheduler(context, runs, reminderTimeRepository, clock)` — a unique
    `PeriodicWorkRequest` named `daily_notification_check`
  - `DailyNotificationWork` — the class WorkManager instantiates. A shell: it pulls three
    collaborators out of `AppContainer` and hands them to `DailyNotificationWorker`
- **Async contract**: synchronous. `doWork()` already runs on a WorkManager background thread,
  so the repository's blocking calls need no dispatcher there.

### Why the next run is a moment and not a delay
A `PeriodicWorkRequest` repeats every twenty-four hours of *elapsed* time, and WorkManager works
each run out from the previous one. That is not "every day at eight". An hour lost to a
daylight-saving change, or a run the device deferred while dozing, moves every following run by
the same amount, and nothing brings it back. So the request names the moment —
`setNextScheduleTimeOverride(next occurrence of the chosen hour, in the zone the device is in
now)` — and the check re-aims itself at the end of every run.

### Why two policies
`ensureDailyCheck` (`KEEP`) is what a launch calls: if a check is armed, even one that is
already late, it is left alone. Re-laying it with `CANCEL_AND_REENQUEUE` on every `onCreate`
cancelled deferred runs — unlock at 08:30 after a dozing night, open the app, and a list due
today lost its notification. `rescheduleDailyCheck` (`UPDATE` plus the override) is what a
changed hour calls, and what the check calls on itself; `UPDATE` moves the work without
cancelling it, so a run may call it from inside itself and have the new moment apply to the next
one.

## Files
- `domain/ListNotification.kt`, `domain/ListNotifier.kt`, `domain/NotificationScheduler.kt`
- `domain/ComputePendingNotificationsUseCase.kt` — the due-today / target-tomorrow decision
- `domain/DailyCheckSchedule.kt` — milliseconds to the next occurrence of an hour in a zone
- `domain/DailyNotificationWorker.kt` — read → compute → post → re-aim
- `data/AndroidListNotifier.kt` — excluded from Pitest
- `data/WorkManagerNotificationScheduler.kt` — excluded from Pitest
- `DailyNotificationWork.kt` — the WorkManager entry point, at the package root because it
  depends on both the container and the domain
- `ui/TodoListsActivity.kt` — `ensureDailyCheck()` on launch; the permission ask
- `ui/NotificationAsk.kt` — whether to ask (`shouldAskForNotifications`) and whether the answer
  counts (`answeredTheAsk`)
- `AndroidManifest.xml` — `POST_NOTIFICATIONS` only
- `res/drawable/ic_checklist.xml` — no `?attr/` tint at the root; the system inflates
  notification drawables without an app theme
- `res/values/strings.xml` — `notification_channel_name`, the one word this feature shows

## Invariants & contracts
- The domain never calls `LocalDate.now()`; "today" comes from `Clock.today()`, in the device's
  local zone.
- A list matches at most one variant: `dueDate` and `targetDate` are mutually exclusive in the
  domain (→ `list-due-date.md`).
- `DailyNotificationWorker.execute()` re-aims the next run last, after posting, and does so even
  when there is nothing to post.
- Notifications are tagged by list id on one shared notification id, so each list holds at most
  one slot and two lists never collide.
- The deep-link intent carries `LIST_ID`, and `TodoListsActivity` consumes it once — a saved
  window that recorded no open page must not fall through to it, or the list re-opens on every
  rotation for the life of the task.
- Notification small icons must not use `?attr/` at the drawable root.
- `AndroidListNotifier` and `WorkManagerNotificationScheduler` are excluded from Pitest: they
  reach Android framework singletons that only Robolectric can stand in for, and Robolectric
  tests are excluded from mutation.
- The ask is spent only when the reader answers. A dialog taken away by the back key or an
  incoming call reports what a refusal reports, so `answeredTheAsk` reads the rationale flag
  either side of the request — the platform moves it only on a choice.

## UI
- **Screens**: none of its own. `TodoListsActivity` arms the check and raises the ask; the hour
  is chosen on the reminder slip (→ `reminder-hour.md`).
- **Words**: the channel name, and nothing else. A notification body is an emoji and a date —
  ⏰ for a due date, 📅 for a target date — written through
  `DateFormat.getBestDateTimePattern(locale, "dM")` so day/month order follows the device.
