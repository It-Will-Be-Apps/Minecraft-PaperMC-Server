#!/bin/bash

set -eux

if ! docker inspect minecraft-server >/dev/null 2>&1; then
    echo "OFFLINE"
    exit 0
fi

CONTAINER_RUNNING=$(docker inspect -f '{{.State.Running}}' minecraft-server)

if [ "$CONTAINER_RUNNING" != "true" ]; then
    echo "OFFLINE"
    exit 0
fi

echo "RUNNING"

docker compose -f /opt/minecraft/compose.yaml exec -T minecraft-server rcon-cli list
