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
- [Reorder Todo Lists](reorder-todo-lists.md) — always-visible drag handle on each list row; handle-only drag (no long-press) reorders lists; position persisted in Room (migration 4→5); long-press on row disabled
- [Empty-State Icon Fix](empty-state-icon-fix.md) — replaces incoherent three-path white-hardcoded ic_checklist with a coherent Material clipboard-with-checkmark tinted via colorOnSurfaceVariant
- [Inline List Create](inline-list-create.md) — FAB opens an ephemeral tinted inline row pinned below the toolbar instead of a modal dialog; FAB and empty state hide while the row is active
- [Create List — Insert at Top](create-list-top-insert.md) — newly created lists are inserted at position 0, pushing existing lists down; drag-to-reorder remains the only subsequent reordering mechanism
- [Lists Screen Watermark Icon](lists-screen-watermark.md) — permanent 200dp/alpha=0.15 background watermark plus empty-state 120dp overlay using ic_format_list_bulleted, matching the dual-layer pattern of the items screen
- [All-Done List Status](all-done-list-status.md) — lists screen highlights fully-completed lists with colorSecondaryContainer background and strikethrough on the list name; empty lists never trigger the treatment
- [List Item Count Badges](list-item-count-badges.md) — each list row shows active and completed item counts as two pill-shaped badges (e.g. "3 / 2"); empty lists show "0 / 0"
- [Inset Item Dividers](inset-item-dividers.md) — 1dp inset dividers between consecutive item rows on Screen 2; skips rows adjacent to the section-header; aligns with title text by insetting 52dp past the drag handle
- [Add Item — Insert at Bottom](add-item-bottom-insert.md) — new items appended at the end of the active section (position = active count); lists still insert at top; SPEC updated to reflect both behaviors consistently
- [Inline List Create — IME Key Submit Fix](inline-list-ime-submit.md) — extends the inline list-name field to accept IME_ACTION_UNSPECIFIED and KEYCODE_ENTER in addition to IME_ACTION_DONE so all keyboards commit the new list on their primary action key
