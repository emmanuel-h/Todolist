---
name: ship
description: Commit all current changes and push to origin. Optionally pass a commit message as argument.
disable-model-invocation: true
allowed-tools: Bash
argument-hint: "[commit message]"
---

Commit and push the current working tree to origin, then close the associated GitHub issue if one can be identified.

1. Run `git status` and `git diff HEAD` to understand what changed.
2. Run `git log --oneline -5` to match the existing commit message style.
3. Stage all relevant changed files. Never stage `.env`, credential files, or unintended binaries.
4. Determine the commit message:
   - If `$ARGUMENTS` is non-empty, use it verbatim.
   - Otherwise, write one concise imperative sentence describing *why* the change exists, not what it does.
5. Commit: `git commit -m "..."`. No co-author trailer. No extra metadata.
6. Push: `git push`. If no upstream is set, use `git push -u origin HEAD`.
7. Detect the GitHub issue to close (try in order, stop at first match):
   a. Extract an issue number from `$ARGUMENTS` — match a bare `#<N>`, a URL fragment `/issues/<N>`, or a plain integer if `$ARGUMENTS` looks like an issue reference.
   b. Extract an issue number from the current branch name — match patterns like `issue-<N>`, `<N>-<slug>`, or `feature/<N>-<slug>`.
   c. Scan the last 10 commit messages (`git log --oneline -10`) for `closes #<N>`, `fixes #<N>`, or `resolves #<N>` (case-insensitive).
   If a number is found, run `gh issue close <N> --comment "Fixed in $(git rev-parse --short HEAD)."`.
   If no number is found, skip silently.
8. Output the commit hash, message, remote URL, and (if applicable) which issue was closed. Nothing else.

Never force-push. Never use `--no-verify`.
