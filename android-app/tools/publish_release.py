#!/usr/bin/env python3
"""Upload to a draft, validate assets, then publish. Public releases are immutable."""
import hashlib
import json
import os
import re
import urllib.error
import urllib.request
from pathlib import Path


class GitHub:
    def __init__(self, token, repository):
        if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repository):
            raise ValueError("Invalid repository")
        self.token = token
        self.base = f"https://api.github.com/repos/{repository}"

    def request(self, path, method="GET", body=None, content_type="application/json", missing_ok=False):
        url = self.base + path if path.startswith("/") else path
        if not (url.startswith(self.base + "/") or url.startswith("https://uploads.github.com/repos/")):
            raise ValueError("Unexpected GitHub API endpoint")
        if body is not None and not isinstance(body, bytes):
            body = json.dumps(body).encode()
        request = urllib.request.Request(url, data=body, method=method, headers={
            "Authorization": f"Bearer {self.token}", "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28", "Content-Type": content_type,
            "User-Agent": "KanaTrainer-Release",
        })
        try:
            with urllib.request.urlopen(request, timeout=120) as response:
                raw = response.read()
                return json.loads(raw) if raw else None
        except urllib.error.HTTPError as error:
            if error.code == 404 and missing_ok:
                return None
            # Do not print request headers, tokens, or response payloads.
            raise RuntimeError(f"GitHub API {method} failed: HTTP {error.code}") from None


def publish(api, directory, commit):
    manifest = json.loads((directory / "update.json").read_text(encoding="utf-8"))
    code = manifest["versionCode"]
    tag = f"android-{code}"
    apk_name = manifest["apkUrl"].rsplit("/", 1)[1]
    if Path(apk_name).name != apk_name or not apk_name.endswith(".apk"):
        raise ValueError("Invalid APK asset")
    apk = directory / apk_name
    if apk.stat().st_size != manifest["sizeBytes"] or hashlib.sha256(apk.read_bytes()).hexdigest() != manifest["sha256"]:
        raise ValueError("APK does not match manifest")
    if api.request("/git/ref/heads/main")["object"]["sha"] != commit:
        print("Skip: this commit is no longer the main branch head.")
        return
    existing = api.request(f"/releases/tags/{tag}", missing_ok=True)
    if existing and not existing["draft"]:
        print(f"Skip: {tag} is already published; its assets will not be overwritten.")
        return
    latest = api.request("/releases/latest", missing_ok=True)
    if latest:
        match = re.fullmatch(r"android-(\d+)", latest["tag_name"])
        if not match or int(match[1]) >= code:
            raise ValueError("Refusing to replace latest with an older/ambiguous version. Review versionCode/history.")
    release_data = dict(
        tag_name=tag, name=f"Android {manifest['versionName']} ({code})",
        body=manifest["notes"], draft=True, prerelease=False, target_commitish=commit,
    )
    release = existing or api.request("/releases", "POST", release_data)
    release_id = release["id"]
    assets = {apk_name: apk.read_bytes(), "update.json": (directory / "update.json").read_bytes()}
    # Only this unpublished draft may be resumed/replaced after a failed run.
    for asset in release["assets"]:
        if asset["name"] in assets:
            api.request(f"/releases/assets/{asset['id']}", "DELETE")
    upload_url = release["upload_url"].split("{")[0]
    for name, payload in assets.items():
        result = api.request(upload_url + "?name=" + name, "POST", payload, "application/octet-stream")
        if result["state"] != "uploaded" or result["size"] != len(payload):
            raise ValueError("Release asset upload incomplete; draft left unpublished")
        digest = result.get("digest")
        if digest and digest != "sha256:" + hashlib.sha256(payload).hexdigest():
            raise ValueError("GitHub asset digest mismatch; draft left unpublished")
    if api.request("/git/ref/heads/main")["object"]["sha"] != commit:
        print("Skip publication: a newer commit arrived. Uploaded assets remain in a draft.")
        return
    api.request(f"/releases/{release_id}", "PATCH", dict(draft=False, make_latest="true",
        body=manifest["notes"], name=f"Android {manifest['versionName']} ({code})"))
    print(f"Published {tag} with APK and update.json.")


if __name__ == "__main__":
    publish(GitHub(os.environ["GH_TOKEN"], os.environ["GITHUB_REPOSITORY"]),
            Path(os.environ["RELEASE_ASSET_DIR"]), os.environ["GITHUB_SHA"])
