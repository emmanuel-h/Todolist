#!/usr/bin/env python3
"""Reshoot every Play Store screenshot, in every listing language, on one emulator.

    python3 reshoot-store.py [serial]

Writes store-assets/screenshots/<locale>/ for en-US and fr-FR: five phone shots
and three shots at each tablet size. The debug build has to be installed on the
emulator first (./gradlew installDebug). The emulator is resized for each form
factor and put back as it was at the end — see store-assets/README.md.
"""
import os, shutil, subprocess, sys, tempfile

TOOLS = os.path.dirname(os.path.abspath(__file__))
SCREENSHOTS = os.path.join(os.path.dirname(TOOLS), "screenshots")
SDK = os.environ.get("ANDROID_HOME") or os.path.expanduser("~/Android/Sdk")
ADB = f"{SDK}/platform-tools/adb"

LOCALES = {"en-US": [], "fr-FR": ["--fr"]}

FORM_FACTORS = [
    ("phone", "1080x1920", "420", "todo_database", "", [
        ("lists", "phone-01-lists"), ("items", "phone-02-items"),
        ("date", "phone-03-calendar"), ("lists-dark", "phone-04-lists-dark"),
        ("items-dark", "phone-05-items-dark")]),
    ("tablet7", "1200x1920", "320", "todo_database_big", "tablet-7", [
        ("lists", "tablet7-01-lists"), ("items", "tablet7-02-items"),
        ("lists-dark", "tablet7-03-lists-dark")]),
    ("tablet10", "1600x2560", "320", "todo_database_big", "tablet-10", [
        ("lists", "tablet10-01-lists"), ("items", "tablet10-02-items"),
        ("lists-dark", "tablet10-03-lists-dark")]),
]


def run(*args, env=None):
    subprocess.run(list(args), check=True, env=env)


def adb(dev, *args):
    run(ADB, "-s", dev, "shell", *args)


def main():
    dev = sys.argv[1] if len(sys.argv) > 1 else "emulator-5554"
    work = tempfile.mkdtemp(prefix="reshoot-")
    try:
        for locale, flags in LOCALES.items():
            seeds = os.path.join(work, locale)
            os.makedirs(seeds)
            run("python3", f"{TOOLS}/make-demo-database.py", f"{seeds}/todo_database", *flags)
            run("python3", f"{TOOLS}/make-demo-database.py", f"{seeds}/todo_database_big", "--big", *flags)

            for prefix, size, density, seed, subdir, shots in FORM_FACTORS:
                adb(dev, "wm", "size", size)
                adb(dev, "wm", "density", density)
                out = os.path.join(work, "out", locale, prefix)
                env = dict(os.environ, SEED_DIR=seeds, SEED_DB=seed, LOCALE=locale)
                run("python3", f"{TOOLS}/capture-screenshots.py", dev, out, prefix,
                    *[shot for shot, _ in shots], env=env)

                target = os.path.join(SCREENSHOTS, locale, subdir)
                os.makedirs(target, exist_ok=True)
                for shot, name in shots:
                    shutil.copyfile(f"{out}/{prefix}-{shot}.png", f"{target}/{name}.png")
    finally:
        adb(dev, "wm", "size", "reset")
        adb(dev, "wm", "density", "reset")
        adb(dev, "am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command", "exit")
        shutil.rmtree(work)
    print("screenshots written to", SCREENSHOTS)


main()
