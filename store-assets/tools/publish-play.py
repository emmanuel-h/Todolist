#!/usr/bin/env python3
"""Publish to Google Play through the Play Developer Publishing API.

    python3 publish-play.py [--bundle AAB --mapping TXT --track TRACK
                             --release-name NAME --notes-dir DIR]
                            [--listing] [--screenshots] [--feature-graphic]
                            [--validate-only] [--changes-not-sent-for-review]
                            [--dry-run]

Everything goes into one edit, and the edit is committed once at the end, so the
listing never shows half an update. With --bundle, the bundle and its R8 mapping
are uploaded and released on TRACK with the notes in DIR/<language>.txt. With
--listing, the title, short and full description of every language come from
store-assets/listing/title-<language>.txt, short-description-<language>.txt and
full-description-<language>.txt. With --screenshots, every screenshot slot of every language under
store-assets/screenshots/<language>/ is emptied and refilled. With
--feature-graphic, store-assets/listing/feature-graphic-<language>.png replaces
the feature graphic of every language that has a screenshots directory.

The service account key is read from $PLAY_SERVICE_ACCOUNT, or
~/.config/todolist/play-service-account.json — see store-assets/README.md.
"""
import argparse, glob, json, os, sys, time

import jwt, requests

PKG = "fr.mandarine.todolist"
SCOPE = "https://www.googleapis.com/auth/androidpublisher"
API = f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{PKG}"
UPLOAD = f"https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/{PKG}"
SCREENSHOTS = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "screenshots")
LISTING = os.path.join(os.path.dirname(SCREENSHOTS), "listing")
LIMITS = [("title", "title", 30), ("shortDescription", "short-description", 80),
          ("fullDescription", "full-description", 4000)]
DEFAULT_KEY = os.path.expanduser("~/.config/todolist/play-service-account.json")

SLOTS = [
    ("phoneScreenshots", "phone-*.png"),
    ("sevenInchScreenshots", "tablet-7/*.png"),
    ("tenInchScreenshots", "tablet-10/*.png"),
]


class PlayError(Exception):
    pass


def access_token(key_path):
    with open(key_path) as f:
        key = json.load(f)
    now = int(time.time())
    assertion = jwt.encode(
        {"iss": key["client_email"], "scope": SCOPE, "aud": key["token_uri"],
         "iat": now, "exp": now + 3600},
        key["private_key"], algorithm="RS256")
    r = requests.post(key["token_uri"], timeout=60, data={
        "grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer",
        "assertion": assertion})
    if not r.ok:
        raise PlayError(f"sign-in refused: {r.status_code} {r.text}")
    return r.json()["access_token"]


class Play:
    def __init__(self, token, dry_run):
        self.dry_run = dry_run
        self.session = requests.Session()
        self.session.headers["Authorization"] = f"Bearer {token}"

    def call(self, method, url, **kw):
        print(f"  {method} {url.split(PKG, 1)[1] or '/'}")
        if self.dry_run:
            return {"id": "dry-run", "versionCode": 0}
        r = self.session.request(method, url, timeout=600, **kw)
        if not r.ok:
            raise PlayError(f"{method} {url}: {r.status_code} {r.text}")
        return r.json() if r.content else {}

    def upload(self, url, path, content_type):
        print(f"    ← {os.path.relpath(path)}")
        if self.dry_run:
            return self.call("POST", url)
        with open(path, "rb") as f:
            return self.call("POST", url, params={"uploadType": "media"}, data=f,
                             headers={"Content-Type": content_type})


def release_notes(notes_dir):
    notes = []
    for path in sorted(glob.glob(f"{notes_dir}/*.txt")):
        text = open(path, encoding="utf-8").read().strip()
        language = os.path.basename(path)[:-len(".txt")]
        if len(text) > 500:
            raise PlayError(f"{language} release notes are {len(text)} characters; Play takes 500")
        notes.append({"language": language, "text": text})
    if not notes:
        raise PlayError(f"no <language>.txt release notes in {notes_dir}")
    return notes


def publish_bundle(play, edit, args):
    notes = release_notes(args.notes_dir)
    bundle = play.upload(f"{UPLOAD}/edits/{edit}/bundles", args.bundle, "application/octet-stream")
    code = bundle["versionCode"]
    play.upload(f"{UPLOAD}/edits/{edit}/apks/{code}/deobfuscationFiles/proguard",
                args.mapping, "application/octet-stream")
    play.call("PUT", f"{API}/edits/{edit}/tracks/{args.track}", json={
        "track": args.track,
        "releases": [{"name": args.release_name, "versionCodes": [str(code)],
                      "status": "completed", "releaseNotes": notes}]})


def listing_languages():
    languages = sorted(d for d in os.listdir(SCREENSHOTS)
                       if os.path.isdir(os.path.join(SCREENSHOTS, d)))
    if not languages:
        raise PlayError(f"no <language>/ directories in {SCREENSHOTS}")
    return languages


def listing_text(language):
    text = {}
    for field, stem, limit in LIMITS:
        value = open(os.path.join(LISTING, f"{stem}-{language}.txt"), encoding="utf-8").read().strip()
        if len(value) > limit:
            raise PlayError(f"{language} {field} is {len(value)} characters; Play takes {limit}")
        text[field] = value
    return text


def publish_listing(play, edit):
    texts = {language: listing_text(language) for language in listing_languages()}
    for language, text in texts.items():
        play.call("PATCH", f"{API}/edits/{edit}/listings/{language}", json=text)


def publish_feature_graphic(play, edit):
    for language in listing_languages():
        play.call("DELETE", f"{API}/edits/{edit}/listings/{language}/featureGraphic")
        play.upload(f"{UPLOAD}/edits/{edit}/listings/{language}/featureGraphic",
                    os.path.join(LISTING, f"feature-graphic-{language}.png"), "image/png")


def publish_screenshots(play, edit):
    for language in listing_languages():
        for slot, pattern in SLOTS:
            shots = sorted(glob.glob(os.path.join(SCREENSHOTS, language, pattern)))
            if not 2 <= len(shots) <= 8:
                raise PlayError(f"{language} {slot}: {len(shots)} screenshots; Play takes 2 to 8")
            play.call("DELETE", f"{API}/edits/{edit}/listings/{language}/{slot}")
            for shot in shots:
                play.upload(f"{UPLOAD}/edits/{edit}/listings/{language}/{slot}", shot, "image/png")


def main():
    p = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    p.add_argument("--bundle")
    p.add_argument("--mapping")
    p.add_argument("--track", choices=["internal", "alpha", "beta", "production"])
    p.add_argument("--release-name")
    p.add_argument("--notes-dir")
    p.add_argument("--listing", action="store_true")
    p.add_argument("--screenshots", action="store_true")
    p.add_argument("--feature-graphic", action="store_true")
    p.add_argument("--validate-only", action="store_true")
    p.add_argument("--changes-not-sent-for-review", action="store_true")
    p.add_argument("--dry-run", action="store_true")
    args = p.parse_args()

    release = [args.bundle, args.mapping, args.track, args.release_name, args.notes_dir]
    if any(release) and not all(release):
        p.error("--bundle needs --mapping, --track, --release-name and --notes-dir")
    if not (args.bundle or args.listing or args.screenshots or args.feature_graphic):
        p.error("nothing to publish: pass --bundle, --listing, --screenshots, --feature-graphic, or any mix")

    key = os.environ.get("PLAY_SERVICE_ACCOUNT", DEFAULT_KEY)
    play = Play("dry-run" if args.dry_run else access_token(key), args.dry_run)
    edit = play.call("POST", f"{API}/edits")["id"]
    try:
        if args.bundle:
            publish_bundle(play, edit, args)
        if args.listing:
            publish_listing(play, edit)
        if args.screenshots:
            publish_screenshots(play, edit)
        if args.feature_graphic:
            publish_feature_graphic(play, edit)
        if args.validate_only:
            play.call("POST", f"{API}/edits/{edit}:validate")
            play.call("DELETE", f"{API}/edits/{edit}")
            print("valid; nothing was published")
            return
        params = {"changesNotSentForReview": "true"} if args.changes_not_sent_for_review else {}
        play.call("POST", f"{API}/edits/{edit}:commit", params=params)
    except Exception:
        if not args.dry_run:
            play.session.delete(f"{API}/edits/{edit}", timeout=60)
        raise
    print("dry run; nothing was sent" if args.dry_run else "published")


if __name__ == "__main__":
    try:
        main()
    except PlayError as e:
        sys.exit(f"publish-play: {e}")
