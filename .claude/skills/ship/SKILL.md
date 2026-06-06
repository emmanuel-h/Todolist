---
name: ship
description: Commit all current changes and push to origin. Optionally pass a commit message as argument.
disable-model-invocation: true
allowed-tools: Bash
argument-hint: "[commit message]"
---

Commit and push the current working tree to origin.

1. Run `git status` and `git diff HEAD` to understand what changed.
2. Run `git log --oneline -5` to match the existing commit message style.
3. Stage all relevant changed files. Never stage `.env`, credential files, or unintended binaries.
4. Determine the commit message:
   - If `$ARGUMENTS` is non-empty, use it verbatim.
   - Otherwise, write one concise imperative sentence describing *why* the change exists, not what it does.
5. Commit: `git commit -m "..."`. No co-author trailer. No extra metadata.
6. Push: `git push`. If no upstream is set, use `git push -u origin HEAD`.
7. Output the commit hash, message, and remote URL. Nothing else.

Never force-push. Never use `--no-verify`.
