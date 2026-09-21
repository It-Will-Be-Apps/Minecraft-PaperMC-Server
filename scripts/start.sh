#!/bin/bash

set -euo pipefail

PID_FILE=/data/.server.pid

if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
    echo "Already running (PID $(cat "$PID_FILE"))."
    exit 0
fi

mkdir -p /data/logs

nohup /start < /dev/null > /data/logs/console-wrapper.log 2>&1 &

echo $! > "$PID_FILE"

disown

echo "Starting server (PID $(cat "$PID_FILE")). Watch with ./logs.sh"
