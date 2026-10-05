#!/bin/bash

set -euo pipefail
PID_FILE=/data/.server.pid

if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
    echo "Server is still running — stop it first: ./stop.sh"
    exit 1
fi

read -p "This deletes /data/world, /data/world_nether, /data/world_the_end. Continue? [y/N] " confirm

if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
    echo "Aborted."
    exit 1
fi

rm -rf /data/world /data/world_nether /data/world_the_end

echo "World wiped. Everything else preserved."
