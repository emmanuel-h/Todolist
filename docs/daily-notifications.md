# Daily Notifications

## What it does
At 08:00 every morning the app fires one Android notification per list whose due date is today or whose target date is tomorrow. Tapping a notification deep-links directly into that list's detail screen. On Android 13+ the `POST_NOTIFICATIONS` permission is requested once, after the first-launch tutorial has finished or been skipped (immediately on later launches); if denied the app continues silently.

## Architecture
- **Layers**: domain, data, ui (presentation layer unchanged)
- **Key types**:
  - `ListNotification` — sealed class with `DueDateToday(list)` and `TargetDateTomorrow(list)` variants; `notificationId()` returns `list.id.hashCode()`
  - `ListNotifier` — `fun interface`; domain contract for posting a batch of notifications
  - `NotificationScheduler` — `fun interface`; domain contract for scheduling the next daily alarm
  - `ComputePendingNotificationsUseCase(clock)` — pure-Kotlin; takes the full list of `TodoList`, returns only those matching due-date-today or target-date-tomorrow
  - `DailyNotificationWorker` — orchestrator injected with the four dependencies; calls `execute()` which reads lists, computes notifications, posts them, then reschedules
  - `AndroidListNotifier(context)` — posts one `NotificationCompat` notification per item via `NotificationManager`; creates the `"todo_reminders"` channel on first call
  - `AndroidNotificationScheduler(context, clock)` — schedules an exact `AlarmManager.RTC_WAKEUP` alarm for the next 08:00 local time
  - `BootCompletedReceiver` — `BroadcastReceiver`; reschedules the daily alarm after `ACTION_BOOT_COMPLETED`
  - `DailyNotificationReceiver` — `BroadcastReceiver`; wires all dependencies and calls `DailyNotificationWorker.execute()` on each alarm fire
- **Async contract**: synchronous; `TodoListRepository.getAll()` returns `List<TodoList>` directly (not a `Flow`); the entire worker runs on the broadcast receiver thread

## Files
- `app/src/main/java/fr/mandarine/todolist/domain/ListNotification.kt` — sealed class with two notification variants and `notificationId()`
- `app/src/main/java/fr/mandarine/todolist/domain/ListNotifier.kt` — domain interface (`fun interface`) for posting notifications
- `app/src/main/java/fr/mandarine/todolist/domain/NotificationScheduler.kt` — domain interface for scheduling the next alarm
- `app/src/main/java/fr/mandarine/todolist/domain/ComputePendingNotificationsUseCase.kt` — filters lists to those triggering a notification today
- `app/src/main/java/fr/mandarine/todolist/domain/DailyNotificationWorker.kt` — orchestrates repository read → compute → post → reschedule
- `app/src/main/java/fr/mandarine/todolist/data/AndroidListNotifier.kt` — Android `NotificationManager` implementation; excluded from Pitest
- `app/src/main/java/fr/mandarine/todolist/data/AndroidNotificationScheduler.kt` — `AlarmManager` implementation; schedules exact alarm at next 08:00; excluded from Pitest
- `app/src/main/java/fr/mandarine/todolist/data/BootCompletedReceiver.kt` — reschedules alarm on device reboot; excluded from Pitest
- `app/src/main/java/fr/mandarine/todolist/data/DailyNotificationReceiver.kt` — alarm entry point; manually wires all dependencies; excluded from Pitest
- `app/src/main/java/fr/mandarine/todolist/ui/TodoListsActivity.kt` — schedules first daily alarm in `onCreate`; requests `POST_NOTIFICATIONS` once on API 33+, deferred until the tutorial state reaches Dismissed so the system dialog never overlaps the first-launch tour
- `app/src/main/AndroidManifest.xml` — declares both receivers, `RECEIVE_BOOT_COMPLETED` and `POST_NOTIFICATIONS` uses-permission, `SCHEDULE_EXACT_ALARM`
- `app/src/main/res/drawable/ic_checklist.xml` — removed `android:tint="?attr/…"` from vector root so the drawable is safe as a notification small icon
- `app/src/main/res/values/strings.xml` — added `notification_channel_name` (the channel is the only place a localized word appears; notification bodies are language-free)
- `app/build.gradle.kts` — extended Pitest `--excludedClasses` / `--excludedTestClasses` for the four Android-framework data classes
- `docs/SPEC.md` — daily notifications section added; "Reminders or notifications" removed from "Out of scope"
- `app/src/test/java/fr/mandarine/todolist/domain/ComputePendingNotificationsUseCaseTest.kt` — 10 tests; due-date-today, target-date-tomorrow, boundary edge cases
- `app/src/test/java/fr/mandarine/todolist/domain/DailyNotificationWorkerTest.kt` — 6 tests; verifies orchestration order and that reschedule always fires
- `app/src/test/java/fr/mandarine/todolist/data/AndroidListNotifierTest.kt` — 13 tests
- `app/src/test/java/fr/mandarine/todolist/data/AndroidNotificationSchedulerTest.kt` — 6 tests
- `app/src/test/java/fr/mandarine/todolist/data/BootCompletedReceiverTest.kt` — 3 tests
- `app/src/test/java/fr/mandarine/todolist/data/DailyNotificationReceiverTest.kt` — 2 tests
- `app/src/test/java/fr/mandarine/todolist/ui/NotificationPermissionTest.kt` — 2 Robolectric tests; permission requested when not granted on API 34, no re-request when already granted

## Invariants & contracts
- `ComputePendingNotificationsUseCase` compares dates against `clock.now()`; the domain layer must never call `LocalDate.now()` directly.
- A list with `dueDate == today` produces `DueDateToday`; a list with `targetDate == tomorrow` produces `TargetDateTomorrow`. A list cannot match both because `dueDate` and `targetDate` are mutually exclusive at the domain layer (→ see `list-due-date.md`).
- `DailyNotificationWorker.execute()` always calls `scheduler.scheduleNextDailyCheck()` last, even when there are zero notifications to post; this keeps the daily chain alive.
- The initial alarm is set in `TodoListsActivity.onCreate()`. The chain then self-perpetuates: each `DailyNotificationReceiver` invocation reschedules the next alarm. `BootCompletedReceiver` repairs the chain after a reboot.
- Notification small icons must not use `?attr/` references at the drawable root level; the system notification renderer inflates drawables without an app theme. Use `app:tint` at the `ImageView` level for in-app display instead.
- Notification deep-link intent extras are `"LIST_ID"` (`Int`) and `"LIST_NAME"` (`String`), matching `TodoListActivity`'s expected extras.
- `notificationId()` is derived from `list.id.hashCode()`; two notifications for the same list on the same day will collide (last write wins). This is the intended behaviour — each list gets at most one active notification slot.
- The `AndroidNotificationScheduler` uses `setAndAllowWhileIdle` (not `setExact`) so the alarm fires even in Doze mode, at the cost of potentially a few minutes of drift.
- The data-layer classes (`AndroidListNotifier`, `AndroidNotificationScheduler`, `BootCompletedReceiver`, `DailyNotificationReceiver`) are excluded from Pitest because they depend on Android framework singletons that cannot be replaced without instrumented tests.
- `POST_NOTIFICATIONS` is requested at most once; if denied the app never re-requests and shows no rationale UI, in keeping with the icon-only UI rule.
- The notification body contains no words: ⏰ + the due date for `DueDateToday`, 📅 + the target date for `TargetDateTomorrow`, with the date rendered through `DateFormat.getBestDateTimePattern(locale, "dM")` so day/month order follows the device's format locale.

## UI
- **Screen(s)**: `TodoListsActivity` (permission request + initial alarm scheduling only; no new UI elements)
- **Layout file(s)**: none added
- **Design decisions**: No in-app notification settings surface is exposed. The 08:00 delivery time is a hard-coded constant, not a user preference. Silent failure on permission denial is intentional — any rationale dialog or in-app copy would violate the icon-only UI convention.
