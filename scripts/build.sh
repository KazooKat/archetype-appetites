#!/usr/bin/env bash
# Builds the data-only Fabric mod jar by zipping src/main/resources.
# A Fabric mod jar is just a zip with fabric.mod.json + data/ at its root.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
res="$root/src/main/resources"
dist="$root/dist"

version="$(grep -oE '"version"[[:space:]]*:[[:space:]]*"[^"]+"' "$res/fabric.mod.json" \
  | head -1 | sed -E 's/.*"version"[[:space:]]*:[[:space:]]*"([^"]+)".*/\1/')"

jar="$dist/archetype-appetites-${version}.jar"

mkdir -p "$dist"
rm -f "$jar"

( cd "$res" && zip -r -X "$jar" . -x '.*' )

echo "Built $jar"
