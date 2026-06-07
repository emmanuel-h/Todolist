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

- If it outputs `NO_QUESTIONS`: set `$SPEC = $ARGUMENTS` and skip to Step 1.
- If it outputs `QUESTIONS`: each line after that header is a question in the format `<text> | <opt1> | <opt2> [| <opt3>] [| <opt4>]`. Call the `AskUserQuestion` tool with those questions, mapping each pipe-separated segment to the question text and its options array. Wait for the user's answers.

  Then build an enriched spec:

  ```
  $ARGUMENTS

  Clarifications agreed with the user:
  - <question 1>: <answer 1>
  - <question 2>: <answer 2>
  …
  ```

  If the user selected "Other" and provided free text, use that text as the answer. Set `$SPEC` to this enriched text.

## Step 1 — developer agent (TDD logic)

Spawn a **`developer`** subagent (subagent_type = "developer") with the following prompt:

```
Implement this feature: $SPEC

Follow your mandatory workflow: design interfaces/models, write failing tests, implement minimal production code, refactor, then iterate until both quality gates are green (100% JaCoCo line+branch coverage, 100% Pitest mutation score).
```

Wait for the agent to finish. If it reports any failing gate, do not proceed to Step 2 — surface the failure to the user.

## Step 2 — ui agent (UI polish)

Once the developer agent has delivered green gates, spawn a **`ui`** subagent (subagent_type = "ui") with this prompt:

```
The developer agent has just landed the backend logic for this feature: $SPEC

Follow your mandatory workflow: ask the user about layout options first (CLARIFY step), then audit existing layouts, fix all findings, and confirm a clean assembleDebug build.
```

Wait for the agent to finish and surface its output to the user.

## Output

After both agents complete successfully, summarise in one sentence what was built and confirm both gates (coverage + mutation) are green and the build passes.
