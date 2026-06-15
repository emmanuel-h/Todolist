# Feature index

> **Root specification**: [SPEC.md](SPEC.md) — authoritative definition of all screens, behaviors, and planned work. Read this first before making any change.


- [Todo List Screen](todo-list-screen.md) — single-list view with checkbox rows, inline add bar, and empty state
- [Multiple Todo Lists](multiple-lists.md) — home screen for creating, deleting, and navigating between lists
- [Room Persistence](room-persistence.md) — SQLite storage via Room so data survives restarts; migration from v1 to v2
- [Complete Todo](complete-todo.md) — toggle completed state per item with strikethrough + fade, persisted in Room
- [UI Polish](ui-polish.md) — illustrated empty states, fixed inline add bar, M3 theme tokens, dark mode
- [Completed Section](completed-section.md) — completed items move to a timestamped bottom section with a labeled divider; uncompleting returns item to active with no position memory
