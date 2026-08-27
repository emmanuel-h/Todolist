#!/usr/bin/env python3
"""Drive the app on one attached device and write the Play Store screenshots.

    python3 capture-screenshots.py <serial> <outdir> <prefix> <shot>...

A shot is one of: lists, items, date, and the -dark variant of each. The demo
database comes from SEED_DIR (default: this directory) and is named by SEED_DB
(default: todo_database) — see make-demo-database.py.
"""
import os, re, subprocess, sys, time

SDK = os.environ.get("ANDROID_HOME") or os.path.expanduser("~/Android/Sdk")
ADB = f"{SDK}/platform-tools/adb"
PKG = "fr.mandarine.todolist"
SEED = os.environ.get("SEED_DIR", os.path.dirname(os.path.abspath(__file__)))


def sh(dev, *args, timeout=120):
    return subprocess.run([ADB, "-s", dev] + list(args),
                          capture_output=True, text=True, timeout=timeout).stdout


def shell(dev, cmd, timeout=120):
    return sh(dev, "shell", cmd, timeout=timeout)


def screencap(dev, path):
    with open(path, "wb") as f:
        subprocess.run([ADB, "-s", dev, "exec-out", "screencap", "-p"],
                       stdout=f, timeout=120)


def dump(dev):
    for _ in range(4):
        shell(dev, "uiautomator dump /sdcard/win.xml")
        xml = shell(dev, "cat /sdcard/win.xml")
        if "<hierarchy" in xml:
            return xml
        time.sleep(1)
    raise RuntimeError("no ui dump")


def find(xml, *, text=None, desc=None):
    for node in re.finditer(r"<node [^>]*>", xml):
        n = node.group(0)
        if text is not None and f'text="{text}"' not in n:
            continue
        if desc is not None and f'content-desc="{desc}"' not in n:
            continue
        m = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
        x1, y1, x2, y2 = map(int, m.groups())
        return (x1 + x2) // 2, (y1 + y2) // 2
    return None


def need(xml, **kw):
    p = find(xml, **kw)
    if p is None:
        raise RuntimeError(f"not found: {kw}")
    return p


def demo_mode(dev):
    shell(dev, "settings put global sysui_demo_allowed 1")
    b = lambda a: shell(dev, f"am broadcast -a com.android.systemui.demo {a}")
    b("-e command exit")
    b("-e command enter")
    b("-e command clock -e hhmm 0930")
    b("-e command battery -e level 100 -e plugged false -e powersave false")
    b("-e command network -e wifi show -e level 4 -e fully true -e mobile hide -e airplane hide")
    b("-e command notifications -e visible false")
    b("-e command status -e volume hide -e bluetooth hide -e location hide -e alarm hide"
      " -e sync hide -e tty hide -e eri hide -e mute hide -e speakerphone hide"
      " -e managed_profile hide -e zen hide -e vpn hide -e cast hide -e hotspot hide")
    for key in shell(dev, "cmd notification list").split():
        shell(dev, f"cmd notification snooze --for 86400000 '{key}'")


def seed(dev):
    shell(dev, f"am force-stop {PKG}")
    sh(dev, "push", f"{SEED}/" + os.environ.get("SEED_DB", "todo_database"), "/data/local/tmp/todo_database")
    sh(dev, "push", f"{SEED}/tutorial_state.xml", "/data/local/tmp/tutorial_state.xml")
    shell(dev, f"run-as {PKG} sh -c '"
               "rm -f databases/todo_database databases/todo_database-wal databases/todo_database-shm; "
               "mkdir -p databases shared_prefs; "
               "cp /data/local/tmp/todo_database databases/todo_database; "
               "cp /data/local/tmp/tutorial_state.xml shared_prefs/tutorial_state.xml'")


def launch(dev):
    shell(dev, f"am start -n {PKG}/.ui.TodoListsActivity")
    time.sleep(4)


def main():
    dev, outdir, prefix, want = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4:]
    os.makedirs(outdir, exist_ok=True)
    demo_mode(dev)

    for night in ("no", "yes"):
        tag = "-dark" if night == "yes" else ""
        if not any(s.endswith(tag) or (tag == "" and not s.endswith("-dark")) for s in want):
            continue
        shell(dev, f"cmd uimode night {night}")
        time.sleep(1)
        seed(dev)
        launch(dev)

        if f"lists{tag}" in want:
            screencap(dev, f"{outdir}/{prefix}-lists{tag}.png")

        if f"items{tag}" in want:
            xml = dump(dev)
            x, y = need(xml, text="Apartment move")
            shell(dev, f"input tap {x} {y}")
            time.sleep(3)
            screencap(dev, f"{outdir}/{prefix}-items{tag}.png")
            shell(dev, "input keyevent KEYCODE_BACK")
            time.sleep(2)

        if f"date{tag}" in want:
            xml = dump(dev)
            x, y = need(xml, text="Reading list")
            shell(dev, f"input swipe {x // 3} {y} {x * 3 // 2} {y} 300")
            time.sleep(2)
            xml = dump(dev)
            cx, cy = need(xml, desc="Set target date")
            shell(dev, f"input tap {cx} {cy}")
            time.sleep(3)
            screencap(dev, f"{outdir}/{prefix}-date{tag}.png")
            shell(dev, "input keyevent KEYCODE_BACK")
            time.sleep(1)
            shell(dev, "input keyevent KEYCODE_BACK")
            time.sleep(1)

    shell(dev, "cmd uimode night no")
    print("captured", prefix, "->", outdir)


main()
