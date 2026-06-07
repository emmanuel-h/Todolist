---
name: feature
description: Implement a full user-facing feature end-to-end by running the developer agent then the ui agent in sequence.
allowed-tools: Agent, AskUserQuestion
argument-hint: "<feature description>"
---

Implement the feature described in `$ARGUMENTS` end-to-end.

## Step 0 — spec analyst (clarify before coding)

Spawn a **`spec-analyst`** subagent (subagent_type = "spec-analyst") with this prompt:

```
Feature request: $ARGUMENTS
```

Parse its output:

- If it outputs `NO_QUESTIONS_UI_ONLY`: set `$SPEC = $ARGUMENTS`, set `$UI_ONLY = true`, and skip to Step 2.
- If it outputs `NO_QUESTIONS`: set `$SPEC = $ARGUMENTS`, set `$UI_ONLY = false`, and proceed to Step 1.
- If it outputs `QUESTIONS`: each line after that header is a question in the format `<text> | <opt1> | <opt2> [| <opt3>] [| <opt4>]`. Call the `AskUserQuestion` tool with those questions, mapping each pipe-separated segment to the question text and its options array. Wait for the user's answers.

  Then build an enriched spec:

  ```
  $ARGUMENTS

  Clarifications agreed with the user:
  - <question 1>: <answer 1>
  - <question 2>: <answer 2>
  …
  ```

  If the user selected "Other" and provided free text, use that text as the answer. Set `$SPEC` to this enriched text. Set `$UI_ONLY = false`. Proceed to Step 1.

## Step 1 — developer agent (TDD logic)

**Skip this step if `$UI_ONLY = true`.**

Spawn a **`developer`** subagent (subagent_type = "developer") with the following prompt:

```
Implement this feature: $SPEC

Follow your mandatory workflow: design interfaces/models, write failing tests, implement minimal production code, refactor, then iterate until both quality gates are green (100% JaCoCo line+branch coverage, 100% Pitest mutation score).
```

Wait for the agent to finish. If it reports any failing gate, do not proceed to Step 2 — surface the failure to the user.

Capture the full `## Delivered` block from the developer agent output as `$DEVELOPER_OUTPUT`. If the step was skipped, set `$DEVELOPER_OUTPUT = "skipped (UI-only feature)"`.

## Step 2 — ui agent (UI polish)

Spawn a **`ui`** subagent (subagent_type = "ui") with this prompt:

- If `$UI_ONLY = false` (developer agent ran):
  ```
  The developer agent has just landed the backend logic for this feature: $SPEC

  Follow your mandatory workflow: ask the user about layout options first (CLARIFY step), then audit existing layouts, fix all findings, and confirm a clean assembleDebug build.
  ```

- If `$UI_ONLY = true` (developer agent was skipped):
  ```
  Implement the UI for this feature: $SPEC

  This is a UI-only change — no domain/data/ViewModel files need to be touched.

  Follow your mandatory workflow: ask the user about layout options first (CLARIFY step), then audit existing layouts, implement all findings, and confirm a clean assembleDebug build.
  ```

Wait for the agent to finish and surface its output to the user.

Capture the full `## Delivered` block from the ui agent output as `$UI_OUTPUT`.

## Step 3 — docs agent (document what was built)

Spawn a **`docs`** subagent (subagent_type = "docs") with this prompt:

```
Feature spec: $SPEC

Developer agent output:
$DEVELOPER_OUTPUT

UI agent output:
$UI_OUTPUT
```

Wait for the agent to finish.

## Output

After all agents complete successfully, summarise in one sentence what was built. If the developer agent ran, confirm both gates (coverage + mutation) are green. Always confirm the build passes.
