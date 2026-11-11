#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac -d "$OUT" \
 "$ROOT/app/src/main/java/com/saisonflow/app/Episode.java" \
 "$ROOT/app/src/main/java/com/saisonflow/app/QualitySelector.java" \
 "$ROOT/tests/CoreTest.java"
java -cp "$OUT" CoreTest
