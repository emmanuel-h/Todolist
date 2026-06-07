---
name: create-issue
description: Create a GitHub issue on this repository. Pass a title as argument; body is drafted from context.
allowed-tools: Bash
argument-hint: "<issue title>"
---

Create a GitHub issue on the `emmanuel-h/Todolist` repository.

1. The issue title is `$ARGUMENTS`. If empty, ask the user for a title before proceeding.
2. Draft a concise body (2–5 bullet points) based on what is known from the conversation context — what the problem or feature is, why it matters, and any acceptance criteria. Do not pad or speculate beyond what is known.
3. Run:
   ```
   gh issue create --title "<title>" --body "<body>"
   ```
4. Output the issue URL. Nothing else.

Never add labels, assignees, or milestones unless the user explicitly asked for them.
