import io
import json
import unittest
from contextlib import redirect_stdout
from unittest.mock import MagicMock, patch
from urllib.error import HTTPError, URLError

from notify_discord import main, notification, send


class DiscordNotificationTest(unittest.TestCase):
    def env(self, **overrides):
        values = {
            "GITHUB_REF": "refs/heads/stg",
            "GITHUB_REPOSITORY": "example/android",
            "GITHUB_RUN_ID": "123", "GITHUB_RUN_ATTEMPT": "2",
            "GITHUB_SHA": "a" * 40, "GITHUB_ACTOR": "developer",
            "CI_RESULT": "success", "CONFIGURE_RESULT": "success", "RELEASE_RESULT": "success",
            "RELEASE_VERSION_CODE": "101", "ALREADY_UPLOADED": "false",
        }
        values.update(overrides)
        return values

    def test_staging_success_has_metadata_and_attempt_link(self):
        payload = notification(self.env())
        embed = payload["embeds"][0]
        self.assertIn("[STG]", embed["title"])
        self.assertIn("내부 테스트 업로드 완료", embed["title"])
        self.assertEqual("https://github.com/example/android/actions/runs/123/attempts/2", embed["url"])
        fields = {field["name"]: field["value"] for field in embed["fields"]}
        self.assertEqual("101", fields["versionCode"])
        self.assertEqual("stg / internal", fields["브랜치 / 트랙"])
        self.assertEqual({"parse": []}, payload["allowed_mentions"])

    def test_production_success_does_not_claim_public_availability(self):
        embed = notification(self.env(GITHUB_REF="refs/heads/main"))["embeds"][0]
        self.assertIn("[PROD]", embed["title"])
        self.assertIn("출시 제출 완료", embed["title"])
        self.assertIn("심사", embed["description"])

    def test_ci_and_configuration_failures_are_reported_when_release_is_skipped(self):
        for stage, label in [("CI_RESULT", "CI 검사"), ("CONFIGURE_RESULT", "배포 설정")]:
            with self.subTest(stage=stage):
                embed = notification(self.env(**{stage: "failure", "RELEASE_RESULT": "skipped", "RELEASE_VERSION_CODE": ""}))["embeds"][0]
                self.assertIn("배포 실패", embed["title"])
                self.assertIn(label, embed["description"])
                self.assertIn("할당 전", [field["value"] for field in embed["fields"]])

    def test_cancelled_skipped_failed_and_already_uploaded_are_distinct(self):
        cases = [
            ({"RELEASE_RESULT": "cancelled"}, "배포 취소"),
            ({"RELEASE_RESULT": "skipped"}, "배포 미실행"),
            ({"RELEASE_RESULT": "failure"}, "배포 실패"),
            ({"ALREADY_UPLOADED": "true"}, "기존 배포 확인"),
        ]
        for overrides, title in cases:
            with self.subTest(title=title):
                self.assertIn(title, notification(self.env(**overrides))["embeds"][0]["title"])

    @patch("notify_discord.urlopen")
    def test_send_uses_json_and_waits_for_discord_confirmation(self, mock_open):
        mock_open.return_value = MagicMock()
        payload = notification(self.env())
        send("https://discord.com/api/webhooks/123/placeholder", payload)
        request = mock_open.call_args.args[0]
        self.assertTrue(request.full_url.endswith("?wait=true"))
        self.assertEqual("POST", request.method)
        self.assertEqual(payload, json.loads(request.data))
        self.assertEqual(20, mock_open.call_args.kwargs["timeout"])

    @patch("notify_discord.urlopen")
    def test_invalid_destination_is_rejected_before_network(self, mock_open):
        with self.assertRaises(ValueError):
            send("https://example.com/api/webhooks/123/placeholder", {})
        mock_open.assert_not_called()

    @patch("notify_discord.urlopen")
    def test_transport_errors_do_not_expose_webhook(self, mock_open):
        webhook = "https://discord.com/api/webhooks/123/placeholder"
        for error in [HTTPError(webhook, 429, webhook, {}, None), URLError(webhook)]:
            with self.subTest(error=type(error).__name__):
                mock_open.side_effect = error
                with self.assertRaises(RuntimeError) as caught:
                    send(webhook, {})
                self.assertNotIn(webhook, str(caught.exception))

    @patch("notify_discord.send")
    def test_missing_webhook_skips_without_sending(self, mock_send):
        with patch.dict("os.environ", {}, clear=True), redirect_stdout(io.StringIO()):
            self.assertEqual(0, main())
        mock_send.assert_not_called()

    @patch("notify_discord.send", side_effect=RuntimeError("private-placeholder"))
    def test_unexpected_errors_are_not_printed(self, mock_send):
        output = io.StringIO()
        with patch.dict("os.environ", self.env(DISCORD_WEBHOOK_URL="placeholder"), clear=True), redirect_stdout(output):
            self.assertEqual(1, main())
        self.assertNotIn("private-placeholder", output.getvalue())


if __name__ == "__main__":
    unittest.main()
