# The reader picks the hour, and the app's first settings surface

_[#74](https://github.com/emmanuel-h/Todolist/issues/74) · Phase 4.1 of
[the device-feedback plan](device-feedback-plan.md)_

> Configure the hour of the notification

08:00 was a constant, and there was nowhere in the app to disagree with it.

```
  masthead — before                     after
  ┌──────────────────────────────┐      ┌──────────────────────────────┐
  │        To do list            │      │        To do list       [⚙]  │
  └──────────────────────────────┘      └──────────────────────────────┘

  [⚙] lays a slip                       pressing the time opens the hours
  ┌─────────────────────────┐           ┌─────────────────────────┐
  │  Reminders              │           │ When shall I remind you?│
  │                         │           │  0   1   2   3   4   5  │
  │  ⏰  Every day at 08:00 │           │  6   7  (8)  9  10  11  │
  │                         │           │ 12  13  14  15  16  17  │
  │                  Done   │           │ 18  19  20  21  22  23  │
  └─────────────────────────┘           └─────────────────────────┘
```

## The surface

The app had no settings of any kind. This is the first, and it is deliberately one glyph and
one slip — the gear sits in the slot the tour's replay `?` left empty when
[#75](tutorial-removal.md) removed it, so the strip was already built to carry a mark
opposite the masthead. It leaves with the masthead when the pen comes out.

The hour grid is the paper calendar's own idiom: cells ruled like the page, the chosen one
**circled in ink** with the same `circledInInk` mark a day is ringed with.

Whole hours only. The stored value is a **minute of day**, so half-hours can be added later
without migrating anything or touching a layer below `ui/`.

The time is written in the reader's own convention. The ICU skeleton is `jm` — `j` is the
locale's preferred hour field — so a French reader is shown `20:00` and an en-US reader
`8:00 PM`. The first attempt used `HH`, which forces twenty-four hours on everybody.

## The trap, and the trap behind the trap

The issue names one: `ExistingPeriodicWorkPolicy.KEEP` means a changed hour never takes
effect, because the existing work is simply left alone. So it must not be `KEEP`.

It also must not be `UPDATE`, which is the obvious replacement and is wrong. `UPDATE`
replaces the work request but **keeps the period already running**, so the new initial delay
is dropped on the floor and the check still lands at the old hour.

This was caught on a device and could not have been caught any other way that was being
tried. The unit test asserted the work's `generation` had incremented, which proves the
policy was applied and proves nothing about *when the check runs*. Meanwhile `dumpsys
jobscheduler` said, with the hour set to 23:00 at 18:11:

```
TIME=+23h59m20s      # still tomorrow's old hour
```

`CANCEL_AND_REENQUEUE` is the one that moves it:

```
18:13 → TIME=+4h46m   # 23:00
18:14 → TIME=+1h46m   # 20:00, after changing it again
```

### The third trap, found in the audit

_Superseded._ The reasoning above is right about `KEEP` and `UPDATE` on their own, and the
conclusion it drew from them was wrong in two ways.

"Re-laying the schedule on every launch costs nothing" is false. `CANCEL_AND_REENQUEUE`
cancels whatever is pending, including a run the device deferred while dozing and has not got
to yet. Unlock the phone at 08:30 after a quiet night, open the app before the deferred check
runs, and it is cancelled and re-aimed at tomorrow: a list due *today* never gets its
notification.

And "the delay is always computed to the next occurrence of the chosen hour" is only true at
the moment of enqueue. A `PeriodicWorkRequest` repeats on elapsed time, so after a
daylight-saving change — or after any deferred run — every following run has moved by the same
amount, and only the next launch put it back.

What the code does now: `setNextScheduleTimeOverride` names the absolute moment rather than the
gap, which is the API that `UPDATE` does honour. A launch calls `ensureDailyCheck` (`KEEP`), so
a pending run is never disturbed; a changed hour calls `rescheduleDailyCheck` (`UPDATE` + the
override), which moves the next run without cancelling the work; and the check itself calls the
same thing at the end of every run, so the aim is recomputed daily in the zone the device is
actually in.

The test that replaced the generation check asserts the thing that matters: **a changed hour
moves the next run time**, and an unchanged hour leaves it where it was (within a second —
WorkManager stamps the absolute moment at enqueue, so two identical schedules land a few real
milliseconds apart).

## Shape

`domain/` keeps a `ReminderTimeRepository` interface and a use case each way; `data/` stores a
minute-of-day in `SharedPreferences`, the pattern the deleted tutorial repository used;
`presentation/ReminderSettingsViewModel` persists **and then reschedules**, because a
persisted hour the scheduler never hears about is the same bug as `KEEP`. `LocalTime` is API
26 and desugared, and `NewApi` is a fatal lint gate, so `:app:lintDebug` staying clean is part
of the answer rather than incidental.

## A regression found on the way

The hour grid was copied from the paper calendar's day cell, and it ringed the wrong place —
the ink circle fell a rule below the numeral. So did the calendar's, on `main`: the ring was
positioned against the **cell** (a finger tall, 48dp) while the numeral is seated by
`seatOnRule` against the **line pitch** (28dp). Those were the same number until
[#68](line-and-row.md) separated them, and nothing noticed because `ui/` is outside the
mutation gate and the tests assert semantics, not pixels. Both grids now seat the ring on the
rule the numeral is written on, and the calendar's *today* dot with them.

## Gates

1 118 tests green; 100% line and branch on `domain/`, `data/` and `presentation/`; Pitest
200/200 mutants killed; `:app:lintDebug` 0 errors. Driven on a device: the gear opens the
slip, the slip opens the hours, a pick circles and persists across a cold restart, and the
scheduled job moves to the chosen hour.
