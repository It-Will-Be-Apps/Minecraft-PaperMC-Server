#!/bin/bash

set -eu

COMPOSE_FILE="/opt/minecraft/server/compose.yaml"
SERVICE_NAME="minecraft-server"

CONTAINER_ID=$(docker compose -f "$COMPOSE_FILE" ps -q "$SERVICE_NAME")

if [ -z "$CONTAINER_ID" ]; then
    echo "OFFLINE"
    exit 0
fi

CONTAINER_RUNNING=$(docker inspect -f '{{.State.Running}}' "$CONTAINER_ID")

if [ "$CONTAINER_RUNNING" != "true" ]; then
    echo "OFFLINE"
    exit 0
fi

echo "ONLINE"

docker compose -f "$COMPOSE_FILE" exec -T "$SERVICE_NAME" rcon-cli list
