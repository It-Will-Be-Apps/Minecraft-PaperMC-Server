#!/bin/bash

set -eu

COMPOSE_FILE="/opt/minecraft/server/compose.yaml"
SERVICE_NAME="minecraft-server"

if [ "$#" -eq 0 ]; then
    echo "ERROR: No command provided."
    exit 1
fi

COMMAND="$*"

echo "Executing Minecraft command: $COMMAND"

docker compose -f "$COMPOSE_FILE" exec -T "$SERVICE_NAME" rcon-cli "$COMMAND"
