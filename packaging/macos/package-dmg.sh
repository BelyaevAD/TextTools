#!/bin/sh
# Builds the unsigned TextTools 1.0 DMG on macOS (Apple silicon or Intel, whichever JDK 21 is installed).
# Capture and paste into other windows stay Windows-only; the Mac build opens the tray.
# Usage, from the repository root: sh packaging/macos/package-dmg.sh
set -eu

if [ "$(uname -s)" != "Darwin" ]; then
  echo "This script runs on macOS." >&2
  exit 1
fi

ROOT=$(CDPATH= cd -- "$(dirname "$0")/../.." && pwd)
cd "$ROOT"

if [ ! -f ./gradlew ]; then
  echo "gradlew was not found in $ROOT" >&2
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "JDK 21 is required. Install Temurin 21 and retry." >&2
  exit 1
fi

java_line=$(java -version 2>&1 | head -n 1)
echo "$java_line" | grep -q '"21' || {
  echo "JDK 21 is required. Current: $java_line" >&2
  exit 1
}

chmod +x ./gradlew
./gradlew :app:packageDmg --no-daemon
echo "DMG directory: $ROOT/app/build/compose/binaries/main/dmg"
