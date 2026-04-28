#!/usr/bin/env bash
set -euo pipefail

SBOM_PATH="${1:-build/sbom.cdx.json}"

if ! command -v grype >/dev/null 2>&1; then
  echo "grype is not installed. See https://github.com/anchore/grype" >&2
  exit 1
fi

if [ ! -f "$SBOM_PATH" ]; then
  echo "SBOM not found at $SBOM_PATH. Run ./scripts/generate-sbom.sh first." >&2
  exit 1
fi

grype "sbom:$SBOM_PATH"
