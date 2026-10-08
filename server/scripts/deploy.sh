#!/bin/bash

set -eu

SERVER_DIR="/opt/minecraft/server"
PLUGINS_DIR="/opt/minecraft/server/plugins"
COMPOSE_FILE="$SERVER_DIR/compose.yaml"

cd "$SERVER_DIR"
git pull

rm -f "$SERVER_DIR/state/*"

mkdir -p "$PLUGINS_DIR"
aws s3 cp "s3://${PLUGINS_BUCKET_NAME}/" "$PLUGINS_DIR/" --recursive --exclude "" --include ".jar"

docker compose -f "$COMPOSE_FILE" up -d


mkdir -p "/opt/minecraft/server/plugins"
aws s3 cp "s3://minecraft-papermc-server-plugins-bucket/" "/opt/minecraft/server/plugins" --recursive --exclude "" --include ".jar"
