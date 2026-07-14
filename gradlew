#!/usr/bin/env sh
set -eu
GRADLE_VERSION="8.7"
ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BOOTSTRAP_DIR="$ROOT_DIR/.gradle-bootstrap"
GRADLE_HOME="$BOOTSTRAP_DIR/gradle-$GRADLE_VERSION"

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$BOOTSTRAP_DIR"
  ZIP="$BOOTSTRAP_DIR/gradle-$GRADLE_VERSION-bin.zip"
  if [ ! -f "$ZIP" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -fsSL "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP"
    elif command -v wget >/dev/null 2>&1; then
      wget -q "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -O "$ZIP"
    else
      echo "curl or wget is required to bootstrap Gradle" >&2
      exit 1
    fi
  fi
  unzip -q -o "$ZIP" -d "$BOOTSTRAP_DIR"
fi

exec "$GRADLE_HOME/bin/gradle" "$@"
