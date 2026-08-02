#!/usr/bin/env python3
"""Upsert the travel personality survey into Cloud Firestore.

Authentication uses Google Application Default Credentials through gcloud, or
an access token supplied via GOOGLE_OAUTH_ACCESS_TOKEN. No service-account key
is read from the repository.
"""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


EXPECTED_LOCATION = "asia-northeast3"
DEFAULT_DATABASE = "(default)"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", default="gayadi", help="Firebase project ID")
    parser.add_argument("--database", default=DEFAULT_DATABASE, help="Firestore database ID")
    parser.add_argument(
        "--data",
        type=Path,
        default=Path("firebase-data/travel-personality-v1.json"),
        help="Survey JSON path",
    )
    parser.add_argument("--dry-run", action="store_true", help="Validate and print the write plan only")
    return parser.parse_args()


def access_token() -> str:
    token = os.environ.get("GOOGLE_OAUTH_ACCESS_TOKEN", "").strip()
    if token:
        return token

    try:
        completed = subprocess.run(
            ["gcloud", "auth", "application-default", "print-access-token"],
            check=True,
            capture_output=True,
            text=True,
        )
    except (FileNotFoundError, subprocess.CalledProcessError) as error:
        raise RuntimeError(
            "Google ADC 인증이 필요합니다. "
            "gcloud auth application-default login 실행 후 다시 시도하세요."
        ) from error

    token = completed.stdout.strip()
    if not token:
        raise RuntimeError("Google ADC access token을 가져오지 못했습니다.")
    return token


def request_json(url: str, token: str, *, method: str = "GET", payload: dict[str, Any] | None = None) -> dict[str, Any]:
    data = None if payload is None else json.dumps(payload, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=data,
        method=method,
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json; charset=utf-8",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.loads(response.read())
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"Firestore API 요청 실패 ({error.code}): {detail}") from error


def firestore_value(item: Any) -> dict[str, Any]:
    if item is None:
        return {"nullValue": None}
    if isinstance(item, bool):
        return {"booleanValue": item}
    if isinstance(item, int):
        return {"integerValue": str(item)}
    if isinstance(item, float):
        return {"doubleValue": item}
    if isinstance(item, str):
        return {"stringValue": item}
    if isinstance(item, list):
        return {"arrayValue": {"values": [firestore_value(entry) for entry in item]}}
    if isinstance(item, dict):
        return {
            "mapValue": {
                "fields": {key: firestore_value(entry) for key, entry in item.items()}
            }
        }
    raise TypeError(f"지원하지 않는 Firestore 값입니다: {type(item).__name__}")


def validate_source(source: dict[str, Any]) -> None:
    questions = source.get("questions", [])
    results = source.get("results", [])
    dimensions = source.get("dimensions", [])
    if source.get("id") != "travel-personality-v1":
        raise ValueError("설문 ID는 travel-personality-v1이어야 합니다.")
    if len(questions) != 9:
        raise ValueError("설문 문항은 정확히 9개여야 합니다.")
    if len(results) != 8:
        raise ValueError("결과 유형은 정확히 8개여야 합니다.")
    if sorted(question["order"] for question in questions) != list(range(1, 10)):
        raise ValueError("문항 order는 1부터 9까지 중복 없이 존재해야 합니다.")
    dimension_ids = [dimension["id"] for dimension in dimensions]
    if source.get("resultCodeOrder") != dimension_ids or len(dimension_ids) != 3:
        raise ValueError("resultCodeOrder와 dimensions 순서가 일치해야 합니다.")
    dimension_codes = {
        dimension["id"]: {dimension["leftCode"], dimension["rightCode"]}
        for dimension in dimensions
    }
    for dimension_id, valid_codes in dimension_codes.items():
        dimension_questions = [question for question in questions if question["dimension"] == dimension_id]
        if len(dimension_questions) != 3:
            raise ValueError(f"{dimension_id} 차원에는 문항이 정확히 3개여야 합니다.")
        for question in dimension_questions:
            if len(question.get("options", [])) != 2:
                raise ValueError(f"{question['id']} 문항에는 선택지가 정확히 2개여야 합니다.")
            if {option["code"] for option in question["options"]} != valid_codes:
                raise ValueError(f"{question['id']} 문항의 선택지 코드가 차원 정의와 다릅니다.")
    expected_codes = {"PNA", "PNR", "PCA", "PCR", "SNA", "SNR", "SCA", "SCR"}
    if {result["code"] for result in results} != expected_codes:
        raise ValueError("8개 결과 코드 구성이 올바르지 않습니다.")


def document_write(project: str, database: str, path: str, fields: dict[str, Any]) -> dict[str, Any]:
    return {
        "update": {
            "name": f"projects/{project}/databases/{database}/documents/{path}",
            "fields": {key: firestore_value(entry) for key, entry in fields.items()},
        }
    }


def build_writes(project: str, database: str, source: dict[str, Any]) -> list[dict[str, Any]]:
    survey_id = source["id"]
    root = {key: entry for key, entry in source.items() if key not in {"id", "questions", "results"}}
    root.update(
        {
            "questionCount": len(source["questions"]),
            "resultCount": len(source["results"]),
            "updatedAt": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        }
    )

    writes = [document_write(project, database, f"surveys/{survey_id}", root)]
    writes.extend(
        document_write(project, database, f"surveys/{survey_id}/questions/{question['id']}", question)
        for question in source["questions"]
    )
    writes.extend(
        document_write(project, database, f"surveys/{survey_id}/results/{result['code']}", result)
        for result in source["results"]
    )
    return writes


def main() -> int:
    args = parse_args()
    source = json.loads(args.data.read_text(encoding="utf-8"))
    validate_source(source)
    writes = build_writes(args.project, args.database, source)

    if args.dry_run:
        print(f"Validated {args.data}: {len(writes)} documents would be upserted.")
        return 0

    token = access_token()
    encoded_database = urllib.parse.quote(args.database, safe="()")
    database_url = (
        f"https://firestore.googleapis.com/v1/projects/{args.project}/databases/{encoded_database}"
    )
    database = request_json(database_url, token)
    actual_location = database.get("locationId")
    if actual_location != EXPECTED_LOCATION:
        raise RuntimeError(
            f"Firestore 위치가 예상과 다릅니다: expected={EXPECTED_LOCATION}, actual={actual_location}"
        )

    commit_url = f"{database_url}/documents:commit"
    result = request_json(commit_url, token, method="POST", payload={"writes": writes})
    write_count = len(result.get("writeResults", []))
    if write_count != len(writes):
        raise RuntimeError(f"Firestore 적재 개수가 다릅니다: expected={len(writes)}, actual={write_count}")

    print(
        f"Upserted {write_count} documents to projects/{args.project}/databases/{args.database} "
        f"in {actual_location}."
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError, RuntimeError, json.JSONDecodeError) as error:
        print(f"error: {error}", file=sys.stderr)
        raise SystemExit(1) from error
