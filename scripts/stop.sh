#!/bin/bash

set -euo pipefail

PID_FILE=/data/.server.pid

rcon-cli stop || true

if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    echo "Stopping server with RCON..."

    for _ in $(seq 1 30); do
        kill -0 "$PID" 2>/dev/null || { echo "Stopped cleanly via RCON."; break; }
        sleep 1
    done

    if kill -0 "$PID" 2>/dev/null; then
        echo "Still running after 30s — sending SIGKILL..."
        kill -9 "$PID"
        sleep 1
    fi

    rm -f "$PID_FILE"
fi

echo "Server stopped."
