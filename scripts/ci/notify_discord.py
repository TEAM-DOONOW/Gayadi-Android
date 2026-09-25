"""Send a deployment result without logging webhook credentials or response bodies."""

import json
import os
import re
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from play_release import release_target


def notification(env):
    target = release_target(env.get("GITHUB_REF", ""))
    stage = "PROD" if target["track"] == "production" else "STG"
    results = [env.get(key, "skipped") for key in ("CI_RESULT", "CONFIGURE_RESULT", "RELEASE_RESULT")]
    if "failure" in results:
        status, color = "배포 실패", 0xDC2626
        failed = [label for label, result in zip(("CI 검사", "배포 설정", "빌드·업로드"), results) if result == "failure"]
        description = "실패 단계: " + ", ".join(failed) + ". 실행 로그를 확인해주세요."
    elif "cancelled" in results:
        status, color = "배포 취소", 0xD97706
        description = "배포 실행이 취소되었습니다. 업로드 진행 여부는 실행 로그에서 확인해주세요."
    elif all(result == "success" for result in results):
        color = 0x16A34A
        if env.get("ALREADY_UPLOADED") == "true":
            status = "기존 배포 확인"
            description = "대상 트랙에 동일 버전이 있어 재업로드를 건너뛰었습니다."
        elif stage == "PROD":
            status = "프로덕션 출시 제출 완료"
            description = "Google Play 업로드를 완료했습니다. 실제 공개 시점은 심사와 게시 설정에 따릅니다."
        else:
            status = "내부 테스트 업로드 완료"
            description = "Google Play 내부 테스트 트랙에 업로드했습니다. 설치 가능 여부는 Play 처리 상태에 따릅니다."
    else:
        status, color = "배포 미실행", 0x6B7280
        description = "배포 단계가 실행되지 않았습니다. 실행 로그를 확인해주세요."

    run_url = "{}/{}/actions/runs/{}/attempts/{}".format(
        env.get("GITHUB_SERVER_URL", "https://github.com").rstrip("/"),
        env["GITHUB_REPOSITORY"], env["GITHUB_RUN_ID"], env.get("GITHUB_RUN_ATTEMPT", "1"),
    )
    values = [
        ("저장소", env["GITHUB_REPOSITORY"]),
        ("브랜치 / 트랙", env["GITHUB_REF"][len("refs/heads/"):] + " / " + target["track"]),
        ("versionCode", env.get("RELEASE_VERSION_CODE") or "할당 전"),
        ("커밋", env.get("GITHUB_SHA", "")[:7] or "알 수 없음"),
        ("실행자", env.get("GITHUB_TRIGGERING_ACTOR") or env.get("GITHUB_ACTOR") or "알 수 없음"),
    ]
    return {
        "allowed_mentions": {"parse": []},
        "embeds": [{
            "title": "[{}] 가야디 {}".format(stage, status),
            "description": description + "\n[GitHub Actions 로그 보기]({})".format(run_url),
            "url": run_url,
            "color": color,
            "fields": [{"name": name, "value": value[:1024], "inline": True} for name, value in values],
        }],
    }


def send(webhook, payload):
    # Restrict credentials to the Discord endpoint; never include the URL in errors.
    if not re.fullmatch(r"https://(?:canary\.|ptb\.)?discord(?:app)?\.com/api(?:/v[0-9]+)?/webhooks/[0-9]+/[A-Za-z0-9_-]+", webhook):
        raise ValueError("DISCORD_WEBHOOK_URL must be a Discord channel webhook URL.")
    request = Request(
        webhook + "?wait=true", data=json.dumps(payload).encode("utf-8"), method="POST",
        headers={"Content-Type": "application/json", "User-Agent": "Gayadi-Deploy-Notifier/1.0"},
    )
    try:
        with urlopen(request, timeout=20):
            pass
    except HTTPError as error:
        raise RuntimeError("Discord notification failed (HTTP {}).".format(error.code)) from None
    except (URLError, TimeoutError):
        raise RuntimeError("Discord notification connection failed.") from None


def main():
    webhook = os.environ.get("DISCORD_WEBHOOK_URL", "").strip()
    if not webhook:
        print("::warning::DISCORD_WEBHOOK_URL is not configured; deployment notification skipped.")
        return 0
    try:
        send(webhook, notification(os.environ))
    except Exception:
        # Even unexpected library errors can contain the URL/token. Keep logs generic.
        print("::warning::Discord notification failed. Check webhook configuration and channel permissions.")
        return 1
    print("Discord deployment notification sent.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
