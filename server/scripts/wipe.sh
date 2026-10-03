#!/bin/bash

set -euo pipefail

COMPOSE_FILE="/opt/minecraft/server/compose.yaml"
SERVICE_NAME="minecraft-server"
WORLD_DATA="/opt/minecraft/data"

CONTAINER_ID=$(docker compose -f "$COMPOSE_FILE" ps -q "$SERVICE_NAME")

if [ -n "$CONTAINER_ID" ]; then
    CONTAINER_RUNNING=$(docker inspect -f '{{.State.Running}}' "$CONTAINER_ID")

    if [ "$CONTAINER_RUNNING" = "true" ]; then
        echo "ERROR: Minecraft server is still running."
        exit 1
    fi
fi

find "$WORLD_DATA" -mindepth 1 -maxdepth 1 -exec rm -rf -- {} +

echo "Minecraft world data wiped."
