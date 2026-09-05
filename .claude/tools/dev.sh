#!/usr/bin/env bash
# Drive the app on a device and read what is on screen.
# Not part of the build; a helper for verifying ui/ changes, which the gate cannot see.
export PATH=$PATH:/home/manu/Android/Sdk/platform-tools
D="${DEV:-$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')}"
PKG=fr.mandarine.todolist
ACT=$PKG/.ui.TodoListsActivity
SHOT_DIR="${SHOT_DIR:-/tmp/todolist-shots}"
mkdir -p "$SHOT_DIR"

case "$1" in
  fresh)   adb -s $D shell pm clear $PKG >/dev/null; adb -s $D shell am start -n $ACT >/dev/null 2>&1; sleep 4 ;;
  start)   adb -s $D shell am force-stop $PKG; adb -s $D shell am start -n $ACT >/dev/null 2>&1; sleep 3 ;;
  tap)     adb -s $D shell input tap "$2" "$3"; sleep "${4:-2}" ;;
  type)    adb -s $D shell input text "$2"; sleep 1 ;;
  key)     adb -s $D shell input keyevent "$2"; sleep "${3:-2}" ;;
  shot)    adb -s $D exec-out screencap -p > "$SHOT_DIR/$2.png"; echo "$SHOT_DIR/$2.png" ;;
  tree)
    adb -s $D shell uiautomator dump /sdcard/w.xml >/dev/null 2>&1
    adb -s $D shell cat /sdcard/w.xml > /tmp/todolist-w.xml
    python3 - <<'PY'
import re
x = open("/tmp/todolist-w.xml").read()
rows = []
for m in re.finditer(r'<node[^>]*>', x):
    n = m.group(0)
    t = re.search(r' text=(?:"([^"]*)"|\'([^\']*)\')', n)
    c = re.search(r' content-desc=(?:"([^"]*)"|\'([^\']*)\')', n)
    b = re.search(r' bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
    tv = next((g for g in (t.groups() if t else ()) if g), "")
    cv = next((g for g in (c.groups() if c else ()) if g), "")
    label = tv or cv
    if not label.strip() or not b: continue
    l, tp, r, bo = map(int, b.groups())
    kind = "text" if tv else "desc"
    rows.append((tp, l, bo, r, kind, label))
rows.sort()
prev = None
for tp, l, bo, r, kind, label in rows:
    gap = f" gap={tp-prev:4}" if prev is not None else " " * 9
    print(f"y={tp:5}..{bo:<5} x={l:4}..{r:<5} h={bo-tp:4}{gap}  [{kind}] {label[:44]}")
    prev = tp
PY
    ;;
  logcat)  adb -s $D logcat -d -t "${2:-300}" | grep -iE "AndroidRuntime|FATAL|$PKG.*(error|exception)" | head -20; echo "[end]" ;;
  *) echo "usage: dev.sh {fresh|start|tap X Y [wait]|type TEXT|key CODE|shot NAME|tree|logcat}" ;;
esac
