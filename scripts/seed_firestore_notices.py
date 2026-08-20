#!/usr/bin/env python3
"""Validate and upsert published update notices into Cloud Firestore."""

from __future__ import annotations

import argparse
import json
import urllib.parse
from datetime import datetime, timezone
from pathlib import Path

from seed_firestore_survey import (
    DEFAULT_DATABASE,
    EXPECTED_LOCATION,
    access_token,
    document_write,
    request_json,
)


ALLOWED_CATEGORIES = {"update", "notice", "event"}
PUBLISHED_STATUS = "published"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", default="gayadi", help="Firebase project ID")
    parser.add_argument("--database", default=DEFAULT_DATABASE, help="Firestore database ID")
    parser.add_argument(
        "--data",
        type=Path,
        default=Path("firebase-data/notices.json"),
        help="Notice JSON path",
    )
    parser.add_argument("--dry-run", action="store_true", help="Validate and print the write plan only")
    return parser.parse_args()


def validate_source(source: dict) -> list[dict]:
    notices = source.get("notices")
    if not isinstance(notices, list) or not notices or any(not isinstance(notice, dict) for notice in notices):
        raise ValueError("업데이트 소식이 하나 이상 필요합니다.")
    ids = [notice.get("id") for notice in notices]
    if len(set(ids)) != len(ids):
        raise ValueError("업데이트 소식 id가 중복되었습니다.")
    for notice in notices:
        notice_id = notice.get("id")
        for field in ("id", "title", "publishedAt", "summary", "publicationStatus"):
            if not isinstance(notice.get(field), str) or not notice[field].strip():
                raise ValueError(f"{notice_id} 소식의 {field} 값이 없습니다.")
        if notice.get("category") not in ALLOWED_CATEGORIES:
            raise ValueError(f"{notice_id} 소식의 category는 {sorted(ALLOWED_CATEGORIES)} 중 하나여야 합니다.")
        if not isinstance(notice.get("pinned"), bool):
            raise ValueError(f"{notice_id} 소식의 pinned 값이 없습니다.")
        version = notice.get("version")
        if version is not None and (not isinstance(version, str) or not version.strip()):
            raise ValueError(f"{notice_id} 소식의 version 값이 올바르지 않습니다.")
        try:
            datetime.strptime(notice["publishedAt"], "%Y-%m-%d")
        except ValueError as error:
            raise ValueError(f"{notice_id} 소식의 publishedAt은 YYYY-MM-DD 형식이어야 합니다.") from error
        sections = notice.get("sections")
        if not isinstance(sections, list) or not sections:
            raise ValueError(f"{notice_id} 소식은 본문 섹션이 하나 이상 필요합니다.")
        for section in sections:
            if not isinstance(section, dict) or not all(
                isinstance(section.get(field), str) and section[field].strip() for field in ("title", "body")
            ):
                raise ValueError(f"{notice_id} 소식에 비어 있는 섹션이 있습니다.")
    return notices


def main() -> int:
    args = parse_args()
    source = json.loads(args.data.read_text(encoding="utf-8"))
    notices = validate_source(source)
    published = [notice for notice in notices if notice["publicationStatus"] == PUBLISHED_STATUS]
    if not published:
        raise ValueError("publicationStatus가 published인 소식이 없습니다.")
    updated_at = datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")
    writes = []
    for notice in published:
        notice_id = notice["id"]
        fields = {key: value for key, value in notice.items() if key != "id" and value is not None}
        fields["updatedAt"] = updated_at
        writes.append(document_write(args.project, args.database, f"notices/{notice_id}", fields))

    if args.dry_run:
        skipped = len(notices) - len(published)
        print(f"Validated {args.data}: {len(writes)} notices would be upserted ({skipped} draft skipped).")
        return 0

    token = access_token()
    encoded_database = urllib.parse.quote(args.database, safe="()")
    database_url = f"https://firestore.googleapis.com/v1/projects/{args.project}/databases/{encoded_database}"
    database = request_json(database_url, token)
    if database.get("locationId") != EXPECTED_LOCATION:
        raise RuntimeError(
            f"Firestore 위치가 예상과 다릅니다: expected={EXPECTED_LOCATION}, actual={database.get('locationId')}"
        )
    result = request_json(
        f"{database_url}/documents:commit",
        token,
        method="POST",
        payload={"writes": writes},
    )
    if len(result.get("writeResults", [])) != len(writes):
        raise RuntimeError("Firestore 업데이트 소식 적재 개수가 다릅니다.")
    print(f"Upserted {len(writes)} notices to projects/{args.project}.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError, RuntimeError, KeyError, json.JSONDecodeError) as error:
        print(f"error: {error}")
        raise SystemExit(1) from error
