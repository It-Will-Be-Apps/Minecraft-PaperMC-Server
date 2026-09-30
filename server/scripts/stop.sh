#!/bin/bash

set -eu

COMPOSE_FILE="/opt/minecraft/server/compose.yaml"
SERVICE_NAME="minecraft-server"

CONTAINER_ID=$(docker compose -f "$COMPOSE_FILE" ps -q "$SERVICE_NAME")

if [ -z "$CONTAINER_ID" ]; then
    echo "Minecraft is already stopped."
    exit 0
fi

CONTAINER_RUNNING=$(docker inspect -f '{{.State.Running}}' "$CONTAINER_ID")

if [ "$CONTAINER_RUNNING" != "true" ]; then
    echo "Minecraft is already stopped."
    exit 0
fi

docker compose -f "$COMPOSE_FILE" stop -t 60 "$SERVICE_NAME"

echo "Minecraft stopped."

if docker inspect -f '{{.State.Running}}' "$CONTAINER_ID" 2>/dev/null; then
    CONTAINER_RUNNING=$(docker inspect -f '{{.State.Running}}' "$CONTAINER_ID")

    if [ "$CONTAINER_RUNNING" = "true" ]; then
        echo "ERROR: Minecraft container is still running."
        exit 1
    fi
fi
