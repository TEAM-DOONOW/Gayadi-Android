#!/usr/bin/env python3
"""Validate and upsert public legal documents into Cloud Firestore."""

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


EXPECTED_IDS = {"terms-of-service", "privacy-policy"}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", default="gayadi", help="Firebase project ID")
    parser.add_argument("--database", default=DEFAULT_DATABASE, help="Firestore database ID")
    parser.add_argument(
        "--data",
        type=Path,
        default=Path("firebase-data/legal-documents.json"),
        help="Legal document JSON path",
    )
    parser.add_argument("--dry-run", action="store_true", help="Validate and print the write plan only")
    return parser.parse_args()


def validate_source(source: dict) -> list[dict]:
    documents = source.get("documents")
    if (
        not isinstance(documents, list)
        or len(documents) != len(EXPECTED_IDS)
        or any(not isinstance(document, dict) for document in documents)
        or {document.get("id") for document in documents} != EXPECTED_IDS
    ):
        raise ValueError("이용약관과 개인정보처리방침 문서가 각각 하나씩 필요합니다.")
    for document in documents:
        for field in ("title", "version", "effectiveDate", "summary", "reviewNotice", "publicationStatus"):
            if not isinstance(document.get(field), str) or not document[field].strip():
                raise ValueError(f"{document.get('id')} 문서의 {field} 값이 없습니다.")
        sections = document.get("sections")
        if not isinstance(sections, list) or len(sections) < 10:
            raise ValueError(f"{document['id']} 문서는 10개 이상의 본문 섹션이 필요합니다.")
        for section in sections:
            if not all(isinstance(section.get(field), str) and section[field].strip() for field in ("title", "body")):
                raise ValueError(f"{document['id']} 문서에 비어 있는 섹션이 있습니다.")
    return documents


def main() -> int:
    args = parse_args()
    source = json.loads(args.data.read_text(encoding="utf-8"))
    documents = validate_source(source)
    if any(document["publicationStatus"] not in {"prototype", "published"} for document in documents):
        raise ValueError("법적 문서의 publicationStatus는 prototype 또는 published여야 합니다.")
    updated_at = datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")
    writes = []
    for document in documents:
        document_id = document["id"]
        fields = {key: value for key, value in document.items() if key != "id"}
        fields["updatedAt"] = updated_at
        writes.append(document_write(args.project, args.database, f"legalDocuments/{document_id}", fields))

    if args.dry_run:
        print(f"Validated {args.data}: {len(writes)} legal documents would be upserted.")
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
        raise RuntimeError("Firestore 법적 문서 적재 개수가 다릅니다.")
    print(f"Upserted {len(writes)} legal documents to projects/{args.project}.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError, RuntimeError, KeyError, json.JSONDecodeError) as error:
        print(f"error: {error}")
        raise SystemExit(1) from error
