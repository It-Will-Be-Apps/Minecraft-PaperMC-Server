#!/bin/bash

set -eu

SERVER_DIR="/opt/minecraft/server"
COMPOSE_FILE="$SERVER_DIR/compose.yaml"

cd "$SERVER_DIR"
git pull

rm -f "$SERVER_DIR/state/*"

docker compose -f "$COMPOSE_FILE" up -d
