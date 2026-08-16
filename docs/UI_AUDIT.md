# UI/UX Audit — Todolist

**Date:** 2026-08-09
**Method:** Spec review (`docs/SPEC.md`), full layout/code review, and live testing on the API 36 phone emulator (light + dark, portrait + landscape). Every UI claim was cross-checked against the Room database (`todo_database`) pulled from the device. A quick pass was also done on the Pixel Tablet emulator.

**TL;DR:** The app is visually coherent, fast to use, and navigation is trivially easy. But the audit found **one real data-loss bug** (dates picked at list creation are silently discarded), two behaviors that contradict the spec, and a set of discoverability problems that are the predictable cost of the icon-only principle — most of them fixable without adding a single word of text.

---

## 🔴 Functional bugs found while driving the app

### 1. Dates chosen in the inline add row are silently lost (data loss — verified twice against the DB)

In `TodoListsActivity.trySubmit` (`app/src/main/java/fr/mandarine/todolist/ui/TodoListsActivity.kt:180`), `viewModel.createList(name, selectedInlineDate, selectedInlineDueDate)` runs inside `applyAndRender { … }`, which dispatches to a background executor — but the lambda reads the **activity fields** at execution time, and `resetInlineDateButtons()` on the next line nulls those fields immediately, before the executor runs.

Result: the alarm icon turns purple, the user believes the due date is set, and the list is created with `NULL` dates every time. The edit-dialog path works only because it never resets its fields.

The 100%-coverage tests miss it because `createListWithDateForTest` passes the date as a parameter instead of going through the button flow. **Fix is one line:** copy the two fields into locals before the lambda. A regression test that goes through the actual button flow (`tapFab → pick date → submitInlineRowForTest`) would have caught it.

### 2. Restore puts the item at the *top* of the active section

Spec says "moves to the bottom of the active section". Restored "Bread" appeared above "Eggs" and "Butter".

### 3. Completed items are ordered most-recent-first

Spec says "earliest first, most recently completed last". Order in DB (`completedAt`) was Milk→Bread→Eggs→Butter; display was Butter→Eggs→Bread→Milk. Most-recent-first is arguably the *better* UX (what you just ticked stays in reach for undo) — but then the spec should say so; right now one of them is wrong.

### 4. Spec status markers are stale

- Double-tap-to-complete (#1) genuinely does nothing (verified live).
- Edit item (#3, marked "not yet implemented") ships — as a nice inline edit, not the dialog the spec describes.
- Delete item (#2, marked "not yet implemented") ships too.
- Reorder use cases and drag wiring exist for both screens despite #5/#6 being marked open.

Worth a spec-sync pass before an agent "implements" something that already exists.

---

## ✅ What is genuinely good

- **Navigation is a non-issue.** Two levels, tap in, back out, state always preserved (Room). Notifications deep-link into the right list. Nothing to fix.
- **Inline add with retained focus** — after submitting an item the field stays focused, so entering a shopping list is machine-gun fast. This beats the dialog most todo apps use.
- **Inline item editing** — tapping ✎ turns the row itself into an editor. Fast, no context loss. Better than the spec's dialog.
- **Glanceable list badges** (`○3 ●1`) — instantly understandable, and the counts update correctly.
- **All-done lists sink into their own section** with strikethrough + tinted card — a satisfying "done" state the spec doesn't even ask for.
- **The add-row sits exactly where a new item will be inserted** (between active and completed). Subtle and correct.
- **Date semantics**: future/today/overdue tinting, custom `colorWarning` attr, locale-correct ICU formatting, year only when it differs. Dark theme is coherent everywhere.
- **Accessibility groundwork**: 48dp targets throughout, contentDescriptions on every icon button, the date box's description switches between target/due mode.

---

## ⚠️ What is hard to understand (the price of icon-only)

Ranked by how much they will actually hurt:

1. **The two date icons are indistinguishable in meaning.** Calendar = "target", alarm = "due" is an invented distinction; no icon can carry it. Worse, after picking, the **chosen date is invisible in the inline row** — the only feedback is a tint change, there is no way to see which date was picked, and no way to clear it (tapping again just reopens the picker). Picking one silently erases the other (mutual exclusion) with zero feedback. Combined with bug #1, the whole date-at-creation flow currently produces nothing but false confidence. *Suggestion:* once a date is picked, show it as a small dismissible chip in the row (dynamic content — spec-legal text).
2. **Empty lists screen reads as "broken grey placeholder".** The 120dp empty-state icon renders *on top of* the 200dp watermark — same glyph, two sizes, superimposed. A first-time user gets one FAB and a visual glitch. Drop one of the two.
3. **Delete is one accidental tap away from permanent loss.** Item rows pack ✓ ✎ 🗑 side by side; 🗑 deletes instantly, no confirmation, no undo (spec-mandated). A mis-tap aimed at ✎ costs real data. Icon-only-legal fixes: an undo snackbar with just ↩, or swipe-to-delete, which is much harder to trigger accidentally.
4. **The delete-list dialog says nothing about stakes.** A big trash icon, then ✕ / 🗑 buttons — decodable, but "this permanently removes the list *and all its items*" is information no icon conveys. (Note the icon-only principle is already porous here: the system date picker shows "Cancel/OK/August".)
5. **Smaller frictions:**
   - Bare-number dividers ("2") are cryptic until learned, and meaningless to TalkBack.
   - The 38%-alpha pencil on list rows reads as *disabled*, not *secondary*.
   - The ghost add-row is unlabeled for TalkBack (`textGhostHint` is `importantForAccessibility="no"` and the row has no contentDescription — `item_todo_inline_add.xml:23`).
   - Completion state is conveyed to screen readers only via the toggle button's label, not the row.
   - Date urgency is color-only (colorblind users lose it).
   - Phone landscape stretches rows edge-to-edge with the watermark peeking between them (the `sw600dp` width cap does not apply there).

---

## 💡 Functionality worth adding (within the spec's out-of-scope limits)

In rough value order:

1. **Undo snackbar** for item/list deletion (↩ icon only) — the single highest-value addition given no-confirmation deletes.
2. **Swipe gestures**: swipe to complete / swipe to delete. Icon-free by nature, standard on Android, and would allow shrinking the three-button row to one.
3. **Tap (or the spec'd double-tap) anywhere on the row to toggle** — the row is the biggest target on screen and currently does nothing on the items screen.
4. **Ship reorder** (#5/#6) — the drag handles are already drawn; if they are inert on any screen, that is a broken affordance.
5. **Clear-completed** action (🗑 next to the completed divider) — completed sections grow forever.
6. **Per-item due dates** — the list-level date machinery (tinting, notifications, Clock abstraction) would transfer directly.
7. **Notification polish**: configurable hour (08:00 is hardcoded), and consider skipping "due today" notifications for lists that are already all-done — currently a fully-completed list still nags.
8. **Home-screen widget / app shortcut** for quick add — high fit for a capture-fast app.

---

## Priority takeaway

Fix the `trySubmit` race (bug 1) first — it is a two-line change, it is user-visible data loss, and it is invisible to the current quality gates. Add a regression test that exercises the real button flow rather than injecting dates directly.
