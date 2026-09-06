---
name: release-store
description: Automate the full Todolist release workflow — bump semver, build signed AAB, create GitHub release, upload artifact. Use when the user wants to publish a new version to the Play Store.
argument-hint: "[major|minor|hotfix]  (default: minor)"
allowed-tools: Bash Read Edit Glob Grep Write
---

Automate the full Todolist release workflow.

## Input

`$ARGUMENTS` contains the release type: `major`, `minor`, or `hotfix`.
Default to `minor` if the argument is absent or unrecognised.

---

## Step 1 — Parse the release type

```
RELEASE_TYPE="${ARGUMENTS:-minor}"
if [[ "$RELEASE_TYPE" != "major" && "$RELEASE_TYPE" != "minor" && "$RELEASE_TYPE" != "hotfix" ]]; then
  RELEASE_TYPE="minor"
fi
echo "Release type: $RELEASE_TYPE"
```

---

## Step 2 — Read the current version from `app/build.gradle.kts`

```bash
CURRENT_CODE=$(grep -oP 'versionCode\s*=\s*\K[0-9]+' app/build.gradle.kts)
CURRENT_NAME=$(grep -oP 'versionName\s*=\s*"\K[^"]+' app/build.gradle.kts)
echo "Current: versionCode=$CURRENT_CODE  versionName=$CURRENT_NAME"
```

---

## Step 3 — Calculate the next version

Split `CURRENT_NAME` on `.` into MAJOR, MINOR, and PATCH components.
PATCH defaults to 0 if the current name has only two parts.

```bash
IFS='.' read -r VER_MAJOR VER_MINOR VER_PATCH <<< "$CURRENT_NAME"
VER_PATCH="${VER_PATCH:-0}"

case "$RELEASE_TYPE" in
  major)
    VER_MAJOR=$((VER_MAJOR + 1))
    VER_MINOR=0
    VER_PATCH=0
    ;;
  minor)
    VER_MINOR=$((VER_MINOR + 1))
    VER_PATCH=0
    ;;
  hotfix)
    VER_PATCH=$((VER_PATCH + 1))
    ;;
esac

NEW_CODE=$((CURRENT_CODE + 1))
NEW_NAME="${VER_MAJOR}.${VER_MINOR}.${VER_PATCH}"

echo "Next:    versionCode=$NEW_CODE  versionName=$NEW_NAME"
```

---

## Step 4 — Confirm with the user

Show the user the planned version bump before touching any file:

```
Current version : $CURRENT_NAME  (code $CURRENT_CODE)
Next version    : $NEW_NAME      (code $NEW_CODE)
Release type    : $RELEASE_TYPE
```

Ask: **"Proceed with this version bump and release? (yes/no)"**

If the user says no, stop and let them specify the correct release type.

---

## Step 5 — Patch `app/build.gradle.kts`

```bash
sed -i "s/versionCode\s*=\s*${CURRENT_CODE}/versionCode = ${NEW_CODE}/" app/build.gradle.kts
sed -i "s/versionName\s*=\s*\"${CURRENT_NAME}\"/versionName = \"${NEW_NAME}\"/" app/build.gradle.kts
```

Verify:

```bash
grep -E 'versionCode|versionName' app/build.gradle.kts
```

---

## Step 6 — Build the signed App Bundle

```bash
./gradlew bundleRelease
```

The output artifact will be at:
```
app/build/outputs/bundle/release/app-release.aab
```

If the build fails, show the error output to the user and stop.

---

## Step 7 — Commit and push the version bump

```bash
git add app/build.gradle.kts
git commit -m "chore: bump version to ${NEW_NAME} (code ${NEW_CODE})"
git push
```

---

## Step 8 — Create the GitHub release and upload the artifact

```bash
TAG="v${NEW_NAME}"

gh release create "$TAG" \
  --title "$TAG" \
  --generate-notes

gh release upload "$TAG" \
  "app/build/outputs/bundle/release/app-release.aab" \
  --clobber

gh release upload "$TAG" \
  "app/build/outputs/mapping/release/mapping.txt" \
  --clobber
```

---

## Step 9 — Generate Play Store release notes and copy to clipboard

Get all commits since the last release tag (feat/fix only):

```bash
PREV_TAG=$(gh release list --limit 2 --json tagName --jq '.[1].tagName')
git log "${PREV_TAG}..HEAD" --oneline --no-merges \
  | grep -E '^[a-f0-9]+ (feat|fix)' \
  | sed 's/^[a-f0-9]* //' \
  | sed 's/^(feat|fix)(\([^)]*\))?!?:\s*//'
```

**Rewrite those raw commit subjects into user-facing release notes.** Do NOT copy commit messages verbatim.

Rules:
- Describe **what the user can now do or what changed from their perspective**.
- Use **plain language** (no conventional-commit prefixes).
- Use **imperative style** ("Add …", "Fix …", "Improve …").
- Merge commits that describe the same end-user change into a single bullet.
- Omit changes with zero visible impact (refactors, CI changes, test improvements, build system changes).
- If all changes are purely technical, use a single generic line instead:
  - French: `- Améliorations internes et corrections mineures.`
  - English: `- Internal improvements and minor fixes.`

### The 500-character limit

**Play Console rejects any language whose notes exceed 500 characters**, with
`La note de version pour fr-FR est trop longue`. The limit is per language and counts the
body only, not the `<fr-FR>` tags.

French runs 15–25% longer than the same English, so **French is the block that blows the
budget**. Write French first and let its length decide how much detail every bullet carries;
an English block that fits proves nothing about the French one.

Aim for **≤ 460 characters** per language — headroom, because a late wording change is
cheaper than a rejected upload. That is roughly **8 bullets of one line each**.

When over budget, cut in this order:
1. **Qualifiers before bullets** — "even on a long list", "in five-minute steps",
   "including the items on a list". The change survives; the elaboration goes.
2. **Bullets describing an absence** — a removed tour or a deleted gesture is nothing the
   user can go looking for.
3. **Whole bullets**, least visible first. Never merge two unrelated changes into one
   comma-spliced line to save characters; that costs more clarity than it saves space.

### Write, measure, then copy

Write each language body to its own file in your scratchpad directory and **measure before
copying** — never copy an unmeasured block.

```bash
SCRATCH="<your scratchpad directory>"

cat > "$SCRATCH/fr.txt" <<'EOF'
- …
EOF

cat > "$SCRATCH/en.txt" <<'EOF'
- …
EOF

wc -m "$SCRATCH/fr.txt" "$SCRATCH/en.txt"
```

If either count exceeds 500, trim by the order above and measure again. Only once both are
under the limit, assemble the tagged block and copy it:

```bash
{ echo "<fr-FR>"; cat "$SCRATCH/fr.txt"; echo "</fr-FR>"; echo;
  echo "<en-US>"; cat "$SCRATCH/en.txt"; echo "</en-US>"; } > "$SCRATCH/notes.txt"

WAYLAND_DISPLAY=wayland-0 wl-copy < "$SCRATCH/notes.txt"
```

Display the notes to the user with **both character counts**, confirm they are in the
clipboard, and say what was cut to fit if anything was.

---

## Step 10 — Display the artifact download URL

```bash
gh release view "$TAG" --json assets \
  --jq '.assets[] | select(.name | endswith(".aab")) | .url'
```

Display the URL clearly to the user. Also remind the user:
- Upload the `.aab` to **Google Play Console → Production (or Internal testing) → Create new release**.
- The same signing keystore must be used for every future release.

---

## Summary output

```
✓ Version bumped  : $CURRENT_NAME (code $CURRENT_CODE) → $NEW_NAME (code $NEW_CODE)
✓ AAB built       : app/build/outputs/bundle/release/app-release.aab
✓ GitHub release  : https://github.com/emmanuel-h/Todolist/releases/tag/$TAG
✓ Download URL    : <url from step 10>
✓ Release notes   : copied to clipboard
```
