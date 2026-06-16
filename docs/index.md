# Feature index

> **Root specification**: [SPEC.md](SPEC.md) — authoritative definition of all screens, behaviors, and planned work. Read this first before making any change.


- [Todo List Screen](todo-list-screen.md) — single-list view with checkbox rows, inline add bar, and empty state
- [Multiple Todo Lists](multiple-lists.md) — home screen for creating, deleting, and navigating between lists
- [Room Persistence](room-persistence.md) — SQLite storage via Room so data survives restarts; migration from v1 to v2
- [Complete Todo](complete-todo.md) — toggle completed state per item with strikethrough + fade, persisted in Room
- [UI Polish](ui-polish.md) — illustrated empty states, fixed inline add bar, M3 theme tokens, dark mode
- [Completed Section](completed-section.md) — completed items move to a timestamped bottom section with a labeled divider; uncompleting returns item to active with no position memory
- [Edit and Delete Todo](edit-delete-todo.md) — inline title editing (tap ✎ or double-tap title) and immediate delete with colorError affordance; no dialog, no confirmation
- [Reorder Active Todo Items](reorder-todos.md) — always-visible drag handle on every active row; handle-only drag (no long-press) reorders within active section; drag clamped at completed section boundary; position persisted in Room (migration 3→4)
- [Inline-Add Row UX Polish](inline-add-ux-polish.md) — top-divider affordance, raised ghost-text alpha, no-width-jump expanded row, concise empty-state copy
- [Icon-Only UI](icon-only-ui.md) — toolbar removed, dialogs icon-only, completed divider shows count badge only, ghost row "…", empty states illustration-only at 120dp
- [Rename Todo List](rename-todo-list.md) — dimmed pencil icon on each list row opens a pre-filled rename dialog; blank confirm discards silently
- [Drag Handle Alignment Fix](drag-handle-alignment.md) — completed rows use `INVISIBLE` instead of `GONE` on the drag handle so item text stays horizontally aligned with the active section
