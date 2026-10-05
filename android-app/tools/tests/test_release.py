import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from prepare_release import prepare
from publish_release import publish


class FakeGitHub:
    def __init__(self, head="current", latest=None, existing=None, fail_upload=False, move_head=False):
        self.head, self.latest, self.existing = head, latest, existing
        self.fail_upload, self.move_head = fail_upload, move_head
        self.calls = []

    def request(self, path, method="GET", body=None, content_type=None, missing_ok=False):
        self.calls.append((path, method, body))
        if path == "/git/ref/heads/main":
            return {"object": {"sha": self.head}}
        if path == "/releases/latest":
            return self.latest
        if path.startswith("/releases/tags/"):
            return self.existing
        if method == "POST" and path == "/releases":
            return dict(id=1, assets=[], upload_url="https://uploads.github.com/repos/a/b/releases/1/assets{?name}")
        if path.startswith("https://uploads.github.com"):
            if self.fail_upload and path.endswith("update.json"):
                raise RuntimeError("Network interrupted")
            if self.move_head:
                self.head = "newer"
            return dict(state="uploaded", size=len(body), digest="sha256:" + hashlib.sha256(body).hexdigest())
        if method == "PATCH":
            return {}
        raise AssertionError((path, method))

    @property
    def published(self):
        return any(method == "PATCH" and body.get("draft") is False for _, method, body in self.calls)


class ReleaseTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.apk = self.root / "app-release.apk"
        self.apk.write_bytes(b"apk-test-fixture")
        self.metadata = self.root / "output-metadata.json"
        self.data = dict(applicationId="com.konomip.kanatrainer", elements=[dict(
            versionCode=10010, versionName="1.2.0", filters=[])])
        self.metadata.write_text(json.dumps(self.data))
        self.output = self.root / "assets"
        self.manifest = prepare(self.apk, self.metadata, "a/b", 26, "Changes", self.output)

    def test_metadata_comes_from_built_apk(self):
        self.assertEqual(10010, self.manifest["versionCode"])
        self.assertEqual(hashlib.sha256(self.apk.read_bytes()).hexdigest(), self.manifest["sha256"])
        self.assertIn("/releases/download/android-10010/", self.manifest["apkUrl"])
        self.assertEqual(self.apk.stat().st_size, self.manifest["sizeBytes"])

    def test_reject_split_apks_and_wrong_application(self):
        for change in (dict(elements=self.data["elements"] * 2), dict(applicationId="other.app")):
            self.metadata.write_text(json.dumps(self.data | change))
            with self.assertRaises(ValueError):
                prepare(self.apk, self.metadata, "a/b", 26, "", self.output)

    def test_publish_only_after_both_assets_uploaded(self):
        api = FakeGitHub()
        publish(api, self.output, "current")
        self.assertTrue(api.published)
        uploads = [path for path, method, _ in api.calls if path.startswith("https://uploads.github.com")]
        self.assertEqual(2, len(uploads))

    def test_private_source_and_public_distribution_are_separate(self):
        source = FakeGitHub()
        distribution = FakeGitHub(head="public-repo-commit")
        publish(distribution, self.output, "current", source)
        self.assertTrue(distribution.published)
        create = next(body for path, method, body in distribution.calls if path == "/releases" and method == "POST")
        self.assertNotIn("target_commitish", create)
        self.assertTrue(all(path == "/git/ref/heads/main" for path, _, _ in source.calls))

    def test_failed_asset_upload_leaves_draft_unpublished(self):
        api = FakeGitHub(fail_upload=True)
        with self.assertRaises(RuntimeError):
            publish(api, self.output, "current")
        self.assertFalse(api.published)

    def test_rerun_does_not_overwrite_published_release(self):
        api = FakeGitHub(existing=dict(draft=False))
        publish(api, self.output, "current")
        self.assertFalse(any(method != "GET" for _, method, _ in api.calls))

    def test_stale_commit_does_not_publish(self):
        for api in (FakeGitHub(head="newer"), FakeGitHub(move_head=True)):
            publish(api, self.output, "current")
            self.assertFalse(api.published)

    def test_prevents_latest_version_rollback(self):
        api = FakeGitHub(latest=dict(tag_name="android-10011"))
        with self.assertRaises(ValueError):
            publish(api, self.output, "current")
        self.assertFalse(api.published)

    def test_tampered_artifact_is_never_uploaded(self):
        next(self.output.glob("*.apk")).write_bytes(b"changed")
        api = FakeGitHub()
        with self.assertRaises(ValueError):
            publish(api, self.output, "current")
        self.assertEqual([], api.calls)


if __name__ == "__main__":
    unittest.main()
