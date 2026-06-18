# Product Specification — To-do List

## Overview

A personal to-do list Android app. The user manages multiple named lists; each list holds
items that can be checked off. All data persists across restarts via Room/SQLite.

### Design principle — icon-only UI

**All static labels, screen titles, and empty-state copy are forbidden.** Every affordance must be self-explanatory through icons alone. Specifically:
- No toolbar title text on any screen.
- No headline or body text in any empty-state layout — icon only.
- Text-field hints use `"…"` rather than a descriptive label.
- Dynamic content (list names, item titles entered by the user) is exempt.

This principle overrides any contradictory suggestion from a UI agent or the wireframes below.

---

## Navigation

```
TodoListsActivity  ("My Lists")
        │
        │  tap a list row
        ▼
TodoListActivity  ("<list name>")
        │
        │  back / up button
        ▼
TodoListsActivity  ("My Lists")
```

---

## Screen 1 — My Lists

### Empty state

```
┌─────────────────────────────────┐
│  My Lists                       │
├─────────────────────────────────┤
│                                 │
│        ┌───────────────┐        │
│        │ [checklist    │        │
│        │  illustration]│        │
│        └───────────────┘        │
│                                 │
│          No lists yet           │
│    Tap + to create your         │
│          first list.            │
│                                 │
│                             (+) │  ← FAB
└─────────────────────────────────┘
```

### Normal state

```
┌─────────────────────────────────┐
│  My Lists                       │
├─────────────────────────────────┤
│  Groceries                   →  │
│  Work tasks                  →  │
│  Weekend                     →  │
│                                 │
│                             (+) │
└─────────────────────────────────┘
```

### Behaviors

**Create a list**
- Tap the FAB → dialog: text field (hint "List name") + Cancel / Create buttons
- Create is a no-op if the field is blank
- On confirm: new list inserted at the top

**Open a list**
- Tap anywhere on a list row → navigates to Screen 2 for that list

**Edit a list name** — _not yet implemented · [#4](https://github.com/emmanuel-h/Todolist/issues/4)_
- Long-press a row → dialog pre-filled with the current name + Cancel / Save
- On confirm: row label updates immediately; items and position are unaffected

**Delete a list**
- Tap the delete icon on a row → confirmation dialog → on confirm, list and all its items
  are permanently removed (no undo)

**Reorder lists** — _not yet implemented · [#6](https://github.com/emmanuel-h/Todolist/issues/6)_
- Long-press-and-drag a row to reorder
- Explicit position persists across restarts
- New lists are always inserted at the top

### Must NOT happen
- Navigating into a deleted list
- Creating a list with a blank name
- List data lost on restart

---

## Screen 2 — Todo List

### Empty state

```
┌─────────────────────────────────┐
│ ←  Groceries                    │
├─────────────────────────────────┤
│                                 │
│        ┌───────────────┐        │
│        │ [checklist    │        │
│        │  illustration]│        │
│        └───────────────┘        │
│                                 │
│          No items yet           │
│    Type below to add your       │
│          first item.            │
│                                 │
├─────────────────────────────────┤
│  Add an item…            [send] │  ← pinned at bottom
└─────────────────────────────────┘
```

### Normal state (mix of active + completed)

```
┌─────────────────────────────────────┐
│ ←  Groceries                        │
├─────────────────────────────────────┤
│  Milk                  [✓] [✎] [🗑] │
│  Bread                 [✓] [✎] [🗑] │
│  Eggs                  [✓] [✎] [🗑] │
│  ─── Completed (2) ─────────────── │  ← divider, visible only when both
│  ~~Butter~~            [↩] [✎] [🗑] │    sections are non-empty
│  ~~Coffee~~            [↩] [✎] [🗑] │  ← 50% alpha + strikethrough
│                                     │
├─────────────────────────────────────┤
│  Add an item…                [send] │
└─────────────────────────────────────┘
```

### Item row

Each row has three icon buttons on the right:

| Button | Active item | Completed item |
|--------|-------------|----------------|
| [✓]/[↩] | Mark as done → moves to completed section | Restore → moves to bottom of active section |
| [✎] | Open edit dialog | Open edit dialog |
| [🗑] | Delete immediately (no confirmation) | Delete immediately (no confirmation) |

There is no checkbox. The strikethrough + 50% alpha is the sole visual indicator of completion.

### Behaviors

**Add an item**
- Type in the inline bar, tap send (or press Enter/Done on keyboard)
- No-op if blank; bar clears on success
- New item appended at the bottom of the active section

**Complete an item** — _behavior change needed · [#1](https://github.com/emmanuel-h/Todolist/issues/1) · [#2](https://github.com/emmanuel-h/Todolist/issues/2)_
- Tap [✓] or double-tap anywhere on the row → item moves immediately to the completed section
- Completed items are ordered by completion time (earliest first, most recently completed last)
- State persists in Room

**Uncomplete an item** — _behavior change needed · [#1](https://github.com/emmanuel-h/Todolist/issues/1)_
- Tap [↩] or double-tap anywhere on the row → item moves to the bottom of the active section
- No memory of original position

**Edit an item title** — _not yet implemented · [#3](https://github.com/emmanuel-h/Todolist/issues/3)_
- Tap [✎] → dialog pre-filled with the current title + Cancel / Save
- On confirm: row label updates immediately; completion state and position are preserved

**Delete an item** — _not yet implemented · [#2](https://github.com/emmanuel-h/Todolist/issues/2)_
- Tap [🗑] → item is permanently removed immediately
- No confirmation dialog, no undo

**Reorder active items** — _not yet implemented · [#5](https://github.com/emmanuel-h/Todolist/issues/5)_
- Long-press-and-drag an active item row to reorder within the active section
- Completed items cannot be manually reordered (always ordered by completion time)
- Explicit position persists across restarts

### Must NOT happen
- A completed item appearing above any active item
- A blank item being created
- Item data lost on restart or back-navigation
- Reordering affecting the completed section
- The completed section header appearing when there are no completed items

---

## Data & persistence

- All data lives in SQLite via Room
- Deleting a list cascades: items are deleted before the list record
- `TodoItem.id` and `TodoList.id` are UUIDs generated at creation; never editable, never
  supplied by the UI
- Insertion order is the default sort; explicit reorder positions are stored as integers
  and persisted per list (or globally for the lists screen)
- Completion timestamp is stored per item to determine ordering within the completed section

---

## Out of scope

The following will not be added:

- User accounts, sync, or cloud backup
- Due dates or reminders
- Priority levels or tags
- Rich text in titles
- Sharing lists
