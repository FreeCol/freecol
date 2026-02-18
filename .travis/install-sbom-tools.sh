#!/usr/bin/env bash
set -euo pipefail

curl -sSfL https://raw.githubusercontent.com/anchore/syft/main/install.sh | sh -s -- -b "$HOME/bin"
curl -sSfL https://raw.githubusercontent.com/anchore/grype/main/install.sh | sh -s -- -b "$HOME/bin"
export PATH="$HOME/bin:$PATH"
