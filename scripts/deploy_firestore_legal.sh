#!/usr/bin/env bash
set -euo pipefail

project_id="${FIREBASE_PROJECT_ID:-gayadi}"

python3 scripts/seed_firestore_legal.py --project "$project_id"
npx --yes firebase-tools@latest deploy \
  --only firestore:rules \
  --project "$project_id" \
  --non-interactive
