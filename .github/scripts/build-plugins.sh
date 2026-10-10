#!/usr/bin/env bash

set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
DIST_DIR="$REPO_ROOT/dist"

cd "$REPO_ROOT"

rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"

found=0

for gradlew in plugins/*/gradlew; do
    [ -f "$gradlew" ] || continue
    found=1

    plugin_dir="$(dirname "$gradlew")"
    plugin_name="$(basename "$plugin_dir")"
    echo "Building plugin: $plugin_name"

    chmod +x "$gradlew"

    (
        cd "$plugin_dir"
        ./gradlew clean build --no-daemon
    )

    mapfile -t jars < <(find "$plugin_dir/build/libs" -maxdepth 1 -type f -name '*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar')

    if [ "${#jars[@]}" -ne 1 ]; then
        echo "ERROR: Expected exactly one plugin JAR for $plugin_name; found ${#jars[@]}."
        printf '%s\n' "${jars[@]}"
        exit 1
    fi

    cp "${jars[0]}" "$DIST_DIR/${plugin_name}.jar"
    echo "Staged: dist/${plugin_name}.jar"
done

if [ "$found" -ne 1 ]; then
    echo "ERROR: No Gradle wrappers found under plugins/*/gradlew"
    exit 1
fi

echo "All plugin builds completed successfully."
