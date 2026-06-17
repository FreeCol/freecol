#!/usr/bin/env bash
set -euo pipefail

OUT_PATH="${1:-build/sbom.cdx.json}"

if ! command -v syft >/dev/null 2>&1; then
  echo "syft is not installed. See https://github.com/anchore/syft" >&2
  exit 1
fi

mkdir -p "$(dirname "$OUT_PATH")"
syft dir:. -o cyclonedx-json > "$OUT_PATH"
echo "SBOM written to $OUT_PATH"
