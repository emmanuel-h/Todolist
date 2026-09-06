# Second round on the device: seven things that were still wrong

_Issues [#77](https://github.com/emmanuel-h/Todolist/issues/77)–[#82](https://github.com/emmanuel-h/Todolist/issues/82)
· 2026-09-06_

The nine original tickets were driven and validated, and the same reader came back with
seven more observations. Six became tickets; two of the seven were the same complaint said
twice. Every one is a case of the app being *correct* and not *understandable*, which is the
pattern worth noticing in this round.

## What changed

**#77 — the row controls were too big.** They drew at 24dp, the size the glyph family is
drawn at, and three of them in a line out-shouted the writing beside them. Row controls now
draw at `PaperDimens.rowGlyph` (20dp) while the touch target stays 48dp. The back glyph, the
calendar's chevrons and the add line's tick keep 24dp: they are not marginalia beside words.

**#78 — the settings glyph said nothing.** A gear promises a drawer of options; this app has
exactly one setting. It is a **bell** now. `ic_alarm` could not be borrowed — it already means
*due date* on a row, and one mark must not carry two meanings on one page. An earlier gear
attempt turned to mud at 24 units; the bell is a dome, a rim and a clapper, and reads.

**#79 — the reminder time.** Two problems. It did not look pressable, so it carries a pencil
now. And a grid of whole hours cannot say 7:30, so it is a **clock** — an ink circle, twelve
numerals with 12 at the top, `Morning` / `Afternoon` above it, hour then minute at five-minute
steps, with the time written above the face as it forms.

**#80 — the sticky pad tore the line up.** While a list was being written the pad became a
minus; on the items page the same corner becomes a tick that commits. Same shape, opposite
act. The pad confirms now. Nothing was lost: back, a tap on bare paper and dismissing the
keyboard all still put the pen down, and per the spec they keep what was written.

**#81 — the name wrapped early.** Measured: the name's box ended at 223dp while the first
control began at 275dp, so about 52dp of rule was reserved and unusable — `OpenCount` held
`marginColumn` whether or not there was a count. It gives that space up when there is nothing
in it. The name also fills its line now: the page's hand breaks lines `Balanced`, which trades
a full first line for two even ones — right for prose, wrong for a name with marks after it.

**#82 — no visible way to remove a date.** Removing worked the whole time: press the mark that
is already ringed. Nothing said so. A ring reads as *this is the current kind*, not *press me
to remove*, and the reader set a due date, went back to take it off, and concluded it was
impossible. The sheet says `Remove` in words when a day is set. This is the spec's own rule —
where an icon has been tried and does not teach the thing, write the words — and the ring had
been tried.

## Two things found by measuring rather than by reading

**The clock's first shape was unusable.** Twenty-four numerals on two rings, 0–11 outside and
12–23 inside, at `0.72` and `0.50` of the radius. At this sheet's width that is well under a
finger apart, so every outer numeral's 48dp target overlapped the inner numeral at the same
angle; the inner one, composed later and therefore on top, took the tap. Aiming at 7 chose 19.
No pair of radii fixes that at this size — one ring of twelve cannot overlap itself, and the
half of the day is a separate question with its own answer.

**The ink ring was a circle in a box.** `ringPath` took `size.minDimension` as its radius, so
around a wide, short box — a word — it drew a small circle through the middle of the writing
instead of an oval around it. It takes a radius per axis now. On every existing call site —
a day on the calendar, an hour on the face, the completion ring — the box is square, the two
radii are the same number, and it is the circle it always was.

## Gates

1 214 tests green; 100% line and branch on `domain/`, `data/` and `presentation/`; Pitest
201/201 mutants killed; `:app:lintDebug` 0 errors. Every one of the seven was driven on a
device: the bell at 20dp beside a name that now fills its line, the pad committing a list,
the clock building 7:30 and the scheduled job following it to +19h37m, and `Remove` clearing
a date from the calendar sheet.
