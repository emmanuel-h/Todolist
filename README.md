# To-Do List

A small Android to-do list app, drawn as ink on ruled paper. Organize your tasks
into lists, give a list a date, and check things off as you go.

Package `fr.mandarine.todolist` · Kotlin · Jetpack Compose · min SDK 24 · target SDK 36

## Features

- Create and manage multiple to-do lists
- Add, edit, and delete items within each list
- Check/uncheck items to track completion
- Give a list a target date (📅) or a due date (⏰) and get a reminder at 08:00
- Reorder by dragging; delete by swiping, with a few seconds to undo
- A near-wordless interface drawn as ink on ruled paper, in daylight or by lamplight
- English and French, selectable from the per-app language picker

## Tech Stack

- **Language:** Kotlin, entirely Jetpack Compose (no `res/layout/`, no View system)
- **Persistence:** Room · **Scheduling:** WorkManager · **Build:** AGP 9.x, Gradle 9.7.1
- **Java:** 11 source/target with core library desugaring (`java.time` under minSdk 24)

## Getting Started

1. Clone the repository
2. Open in Android Studio (or use the wrapper from the command line)
3. Run on a device or emulator (Android 7.0+)

```bash
./gradlew assembleDebug
```

## Project Structure

Three strict layers; a class never imports from a layer above it, and `domain/`
never imports `android.*`. The palette, dimensions and motion specs are Kotlin
objects in `ui/paper/`, not resources.

```
app/src/main/
├── java/fr/mandarine/todolist/
│   ├── domain/       # Pure Kotlin: models, repository interfaces, use cases
│   ├── data/         # Room + WorkManager implementations
│   ├── presentation/ # ViewModels and UI state
│   └── ui/           # Compose screens and the paper/ design system
└── res/              # Vector icons, strings, a bare window theme
```

`docs/SPEC.md` is the authoritative product definition; `docs/index.md` indexes a
feature note per shipped change. `store-assets/` holds the Play listing and the
scripts that regenerate the icon, feature graphic and screenshots.

## Quality gates

Every feature must reach 100% JaCoCo line+branch coverage and 100% Pitest mutation
score over `domain.*`, `data.*` and `presentation.*` before it is done. `ui.*` is
not mutated — a green gate is not evidence that a `ui/` change was tested.

```bash
./gradlew testDebugUnitTest                        # unit tests
./gradlew createDebugUnitTestCoverageReport        # app/build/reports/coverage/test/debug/report.xml
./gradlew pitest                                   # app/build/reports/pitest/
./gradlew :app:lintDebug                           # NewApi is fatal
./gradlew testDebugUnitTest createDebugUnitTestCoverageReport pitest   # all at once
```

Run a single class with `--tests "fr.mandarine.todolist.SomeTest"`.

---

# Releasing to the Play Store by hand

The `/release-store` skill does all of this in one step. What follows is the same
runbook to drive manually — every command is meant to run from the repo root.

## 0. Prerequisites, once per machine

The release `signingConfig` is only created when all four of these properties
resolve, from `~/.gradle/gradle.properties` (preferred — never commit them) or from
environment variables of the same name:

```properties
RELEASE_KEYSTORE_FILE=/absolute/path/to/upload-key.jks
RELEASE_KEYSTORE_PASSWORD=…
RELEASE_KEY_ALIAS=…
RELEASE_KEY_PASSWORD=…
```

If any one is missing or blank the build still succeeds and quietly produces an
**unsigned** bundle that Play will reject. Check before you start:

```bash
grep -c RELEASE_ ~/.gradle/gradle.properties    # expect 4
```

The same keystore must be used for every release, forever. Also needed: a JDK new
enough for AGP 9 (built here on Temurin 25), the Android SDK, and `gh` authenticated against
`github.com/emmanuel-h/Todolist`.

## 1. Green gates on a clean tree

Do not release anything the gates have not passed.

```bash
git status --short                    # expect empty
git pull
./gradlew testDebugUnitTest createDebugUnitTestCoverageReport pitest
./gradlew :app:lintDebug
```

## 2. Bump the version

Both fields live in `app/build.gradle.kts` under `defaultConfig`. `versionCode`
increments by exactly 1 (Play refuses a code it has already seen); `versionName`
is semver — bump major for a breaking rework, minor for features, patch for a
hotfix.

```bash
grep -E 'versionCode|versionName' app/build.gradle.kts
```

```kotlin
versionCode = 6          // was 5
versionName = "2.2.0"    // was "2.1.0"
```

## 3. Build the signed bundle

```bash
./gradlew bundleRelease
```

Outputs:

| Path | What it is |
|---|---|
| `app/build/outputs/bundle/release/app-release.aab` | the artifact to upload |
| `app/build/outputs/mapping/release/mapping.txt` | R8 deobfuscation map |

Confirm it is actually signed before going further:

```bash
ls -la app/build/outputs/bundle/release/app-release.aab
jarsigner -verify app/build/outputs/bundle/release/app-release.aab | tail -1
```

**The one failure mode to expect here:** `bundleRelease` is the only task that runs
R8 and the release Compose compiler, so a dependency bump can break *this* build
while the entire test gate stays green. If `bundleRelease` fails right after a
dependabot merge, suspect the Compose compiler / BOM pairing in
`gradle/libs.versions.toml` first, not your change.

## 4. Commit, tag, publish on GitHub

```bash
git add app/build.gradle.kts
git commit -m "chore: bump version to 2.2.0 (code 6)"
git push

TAG=v2.2.0
gh release create "$TAG" --title "$TAG" --generate-notes
gh release upload "$TAG" app/build/outputs/bundle/release/app-release.aab --clobber
gh release upload "$TAG" app/build/outputs/mapping/release/mapping.txt --clobber
```

## 5. Write the Play Console release notes

Start from what actually shipped, then rewrite it — never paste commit subjects:

```bash
PREV_TAG=$(gh release list --limit 2 --json tagName --jq '.[1].tagName')
git log "${PREV_TAG}..HEAD" --oneline --no-merges | grep -E '^[a-f0-9]+ (feat|fix)'
```

Rules: say what the *user* can now do, in plain imperative language; merge commits
that describe one end-user change into one bullet; drop refactors, CI, tests and
build changes entirely. If nothing user-visible shipped, use a single line —
`- Améliorations internes et corrections mineures.` / `- Internal improvements and
minor fixes.`

### The 500-character limit is the whole difficulty

Play Console rejects **per language** over 500 characters
(`La note de version pour fr-FR est trop longue`), counting the body only, not the
`<fr-FR>` tags. French runs 15–25% longer than the same English, so **French is
what blows the budget — write French first** and let its length decide how much
detail every bullet carries. Aim for ≤ 460 characters, roughly 8 one-line bullets.

Over budget? Cut in this order:

1. Qualifiers, not bullets — "even on a long list", "in five-minute steps".
2. Bullets describing an absence (a removed tour is nothing to go looking for).
3. Whole bullets, least visible first. Never comma-splice two unrelated changes
   together to save characters.

Write each language to a file and **measure before you paste**:

```bash
S=/tmp/notes && mkdir -p $S
cat > $S/fr.txt <<'EOF'
- …
EOF
cat > $S/en.txt <<'EOF'
- …
EOF
wc -m $S/fr.txt $S/en.txt          # both must be < 500

{ echo "<fr-FR>"; cat $S/fr.txt; echo "</fr-FR>"; echo;
  echo "<en-US>"; cat $S/en.txt; echo "</en-US>"; } > $S/notes.txt
```

## 6. Upload in the Play Console

1. **Play Console → Todolist → Production** (or *Testing → Internal testing* for a
   dry run) **→ Create new release**.
2. Upload `app-release.aab`. App signing by Google Play re-signs it; your keystore
   is the *upload* key.
3. Paste the tagged `notes.txt` block into the release-notes field.
4. Review and roll out. A staged rollout percentage can be raised later from the
   same screen.

Native debug symbols ride inside the bundle (`debugSymbolLevel = "FULL"`), and R8
mapping is uploaded automatically by the bundle, so crash reports arrive
deobfuscated with no extra step.

## 7. When the store listing itself changed

Screenshots, descriptions, icon and feature graphic are versioned under
`store-assets/` and are *not* part of the bundle — they are uploaded separately in
**Store presence → Main store listing**. `store-assets/README.md` has the commands
that regenerate each one; note that the demo database's `TODAY` must be moved
forward before a fresh screenshot run or the dates will have drifted into the past.

## Checklist

```
[ ] clean tree, pulled, all gates green
[ ] versionCode +1 and versionName bumped in app/build.gradle.kts
[ ] bundleRelease succeeded and jarsigner verifies the .aab
[ ] version bump committed and pushed
[ ] GitHub release tagged, .aab + mapping.txt attached
[ ] release notes written French-first, both languages measured < 500 chars
[ ] .aab uploaded to Play Console and rolled out
[ ] store-assets re-uploaded if the listing changed
```

## License

MIT
