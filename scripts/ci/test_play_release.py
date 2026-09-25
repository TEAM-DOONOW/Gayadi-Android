import unittest
from unittest.mock import Mock

from play_release import MAX_VERSION_CODE, already_uploaded, check_play, release_metadata, release_target
from prepare_play_signing import property_value


class ReleaseTargetTest(unittest.TestCase):
    def test_branch_selects_track_and_environment(self):
        self.assertEqual({"track": "internal", "environment": "play-internal"}, release_target("refs/heads/stg"))
        self.assertEqual({"track": "production", "environment": "play-production"}, release_target("refs/heads/main"))

    def test_other_branches_tags_and_pr_refs_cannot_deploy(self):
        for ref in ["", "refs/heads/dev", "refs/heads/feat/#146", "refs/pull/146/merge", "refs/tags/main"]:
            with self.subTest(ref=ref), self.assertRaises(ValueError):
                release_target(ref)


class ReleaseMetadataTest(unittest.TestCase):
    def test_code_increases_and_reruns_keep_identity(self):
        sha = "a" * 40
        first = release_metadata("100", "1", sha)
        self.assertEqual("101", first["version_code"])
        self.assertEqual(first, release_metadata("100", "1", sha))
        self.assertEqual("102", release_metadata("100", "2", sha)["version_code"])
        self.assertNotEqual(first["release_name"], release_metadata("100", "1", "b" * 40)["release_name"])

    def test_rejects_missing_invalid_and_overflowing_inputs(self):
        for base, run, sha in [("", "1", "a" * 40), ("-1", "1", "a" * 40), ("27", "0", "a" * 40), (str(MAX_VERSION_CODE), "1", "a" * 40), ("27", "1", "a\ninjected")]:
            with self.subTest(base=base, run=run), self.assertRaises(ValueError):
                release_metadata(base, run, sha)


class PlayPreflightTest(unittest.TestCase):
    def tracks(self, code=28, name="expected", status="completed", track="internal"):
        return [{"track": track, "releases": [{"versionCodes": [str(code)], "name": name, "status": status}]}]

    def test_new_version_is_allowed(self):
        self.assertFalse(already_uploaded(self.tracks(27), [{"versionCode": 27}], 28, "expected", "internal"))
        self.assertFalse(already_uploaded([], [], 28, "expected", "internal"))

    def test_completed_same_commit_is_idempotent(self):
        self.assertTrue(already_uploaded(self.tracks(), [{"versionCode": 28}], 28, "expected", "internal"))

    def test_rejects_version_owned_by_different_commit(self):
        with self.assertRaisesRegex(ValueError, "different release"):
            already_uploaded(self.tracks(name="other"), [], 28, "expected", "internal")

    def test_rejects_draft_partial_and_upload_without_release(self):
        for status in ["draft", "inProgress", "halted"]:
            with self.subTest(status=status), self.assertRaisesRegex(ValueError, "already exists"):
                already_uploaded(self.tracks(status=status), [], 28, "expected", "internal")
        with self.assertRaisesRegex(ValueError, "already exists"):
            already_uploaded([], [{"versionCode": 28}], 28, "expected", "internal")

    def test_other_track_does_not_count_as_completed_internal_release(self):
        with self.assertRaises(ValueError):
            already_uploaded(self.tracks(track="production"), [], 28, "expected", "internal")

    def test_production_rerun_requires_completed_production_release(self):
        self.assertTrue(already_uploaded(self.tracks(track="production"), [], 28, "expected", "production"))
        with self.assertRaisesRegex(ValueError, "already exists"):
            already_uploaded(self.tracks(track="internal"), [], 28, "expected", "production")
        for status in ["draft", "inProgress", "halted"]:
            with self.subTest(status=status), self.assertRaisesRegex(ValueError, "already exists"):
                already_uploaded(self.tracks(track="production", status=status), [], 28, "expected", "production")

    def test_version_sequence_is_shared_across_tracks(self):
        self.assertFalse(already_uploaded(self.tracks(code=27, track="internal"), [], 28, "expected", "production"))
        self.assertFalse(already_uploaded(self.tracks(code=27, track="production"), [], 28, "expected", "internal"))
        with self.assertRaisesRegex(ValueError, "newer version"):
            already_uploaded(self.tracks(code=30, track="internal"), [], 28, "expected", "production")

    def test_unknown_track_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "Unsupported"):
            already_uploaded([], [], 28, "expected", "beta")

    def test_production_inspection_checks_selected_track(self):
        client = Mock()
        client.request.side_effect = [{"id": "edit"}, {"tracks": self.tracks(track="production")}, {"bundles": []}, {}]
        self.assertTrue(check_play(client, 28, "expected", "production"))
        client.request.assert_called_with("DELETE", "/edits/edit")

    def test_stale_workflow_does_not_replace_newer_release(self):
        with self.assertRaisesRegex(ValueError, "newer version"):
            already_uploaded(self.tracks(code=30), [], 28, "expected", "internal")
        with self.assertRaisesRegex(ValueError, "newer version"):
            already_uploaded([], [{"versionCode": 30}], 28, "expected", "internal")

    def test_edit_is_deleted_without_commit_after_inspection(self):
        client = Mock()
        client.request.side_effect = [{"id": "edit"}, {"tracks": []}, {"bundles": []}, {}]
        self.assertFalse(check_play(client, 28, "expected", "internal"))
        self.assertEqual([("POST", "/edits", {}), ("GET", "/edits/edit/tracks"), ("GET", "/edits/edit/bundles"), ("DELETE", "/edits/edit")], [call.args for call in client.request.call_args_list])

    def test_api_failure_does_not_proceed_and_still_deletes_edit(self):
        client = Mock()
        client.request.side_effect = [{"id": "edit"}, RuntimeError("API unavailable"), {}]
        with self.assertRaisesRegex(RuntimeError, "API unavailable"):
            check_play(client, 28, "expected", "internal")
        client.request.assert_called_with("DELETE", "/edits/edit")


class PropertiesEscapingTest(unittest.TestCase):
    def test_special_characters_cannot_inject_properties(self):
        self.assertEqual(r"\ pass\=word\:\\\nnext\r\t", property_value(" pass=word:\\\nnext\r\t"))
        self.assertEqual(r"\ud55c\uae00", property_value("한글"))
        self.assertEqual(r"\ud83d\ude00", property_value("😀"))


if __name__ == "__main__":
    unittest.main()
