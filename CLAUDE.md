# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android native to-do list app (`fr.mandarine.todolist`). Kotlin, single `:app` module, AGP 9.x, min SDK 24, target SDK 36.

## Commands

```bash
# Run unit tests
./gradlew testDebugUnitTest

# JaCoCo coverage report (XML: app/build/reports/coverage/test/debug/report.xml)
./gradlew createDebugUnitTestCoverageReport

# Pitest mutation report (HTML/XML: app/build/reports/pitest/)
./gradlew pitest

# All quality gates in one shot
./gradlew testDebugUnitTest createDebugUnitTestCoverageReport pitest

# Run a single test class
./gradlew testDebugUnitTest --tests "fr.mandarine.todolist.SomeTest"

# Assemble debug APK
./gradlew assembleDebug
```

## Quality gates

Every feature must reach **100% JaCoCo line+branch coverage** and **100% Pitest mutation score** before it is considered done. The `developer` agent (`.claude/agents/developer.md`) enforces this automatically.

## Product specification

**`docs/SPEC.md` is the authoritative product definition.** Every agent must read it before starting any feature work. It defines all screens, behaviors, invariants, and which behaviors are not yet implemented (with linked GitHub issues). Do not implement anything that contradicts it; if a planned feature conflicts with the spec, flag it to the user before proceeding.

## Agent pipeline

For any user-facing feature, run the agents in this order:

1. **`developer`** — TDD logic: domain, data, ViewModel, unit tests, coverage + mutation gates
2. **`ui`** — Polish: layouts, themes, accessibility, Material Design 3 compliance, build check

The `ui` agent never touches domain/data/ViewModel code. The `developer` agent never touches layout XML or theme files.

## Architecture

Three strict layers — a class must not import from a layer above it or import `android.*` in `domain/`:

```
app/src/main/java/fr/mandarine/todolist/
├── domain/       # Pure Kotlin: models (immutable data class), repository interfaces, use cases
├── data/         # Repository implementations — may use Android APIs
└── presentation/ # ViewModels + UI state sealed classes — depends on domain only
```

Test sources mirror the main layout under `app/src/test/java/fr/mandarine/todolist/`.

## Testing conventions

- Framework: JUnit4 (`@Test`, `@Before`, `@After`)
- Mocking: MockK (`mockk<T>()`, `every`, `coEvery`, `verify`, `coVerify`)
- Coroutines: `kotlinx-coroutines-test` (`runTest`, `TestCoroutineScheduler.advanceTimeBy()`)
- Test name pattern: `` fun `should <expected behaviour> when <condition>`() ``
- Assert behaviour, not implementation; never use `any()` in `verify`

## Pitest setup note

The project uses a custom `JavaExec`-based `pitest` task instead of the Gradle plugin because AGP 9.x does not register `JavaPlugin` via `project.plugins.apply()`, which breaks the plugin's auto-detection. The task is defined in `app/build.gradle.kts` and invokes the Pitest CLI directly against the `debug` variant classpath.

## Code style

- No comments; names must be self-documenting
- Immutable by default (`val`, `data class`, `copy()`)
- Validate at layer boundaries with `require()` / `check()`; no defensive code beyond that
- No nullable returns to signal errors — use `Result<T>` or a domain sealed class

## Skills

- `/ship [message]` — stage, commit, and push in one step (no co-author trailer, no force-push)
