#!/usr/bin/env bash
set -euo pipefail

project_id="${FIREBASE_PROJECT_ID:-gayadi}"

if [[ ! -x node_modules/.bin/firebase ]]; then
  echo "Firebase CLI가 없습니다. npm ci를 먼저 실행하세요." >&2
  exit 1
fi

python3 scripts/seed_firestore_survey.py --project "$project_id"
node_modules/.bin/firebase deploy \
  --only firestore:rules \
  --project "$project_id" \
  --non-interactive
