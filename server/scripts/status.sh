#!/bin/bash

set -eu

#IDLE_SINCE_FILE="/opt/minecraft/server/state/idle-since"
COMPOSE_FILE="/opt/minecraft/server/compose.yaml"
SERVICE_NAME="minecraft-server"

CONTAINER_ID=$(docker compose -f "$COMPOSE_FILE" ps -q "$SERVICE_NAME")

if [ -z "$CONTAINER_ID" ]; then
    jq -n '{
        running: false,
        players: [],
        idle: false,
        idleSince: null
    }'
    exit 0
fi

CONTAINER_RUNNING=$(docker inspect -f '{{.State.Running}}' "$CONTAINER_ID")

if [ "$CONTAINER_RUNNING" != "true" ]; then
    jq -n '{
        running: false,
        players: [],
        idle: false,
        idleSince: null
    }'
    exit 0
fi

RCON_OUTPUT=$(docker compose -f "$COMPOSE_FILE" exec -T "$SERVICE_NAME" rcon-cli list)
PLAYER_COUNT=$(printf '%s\n' "$RCON_OUTPUT" | sed -n 's/^There are \([0-9][0-9]*\) of a max of [0-9][0-9]* players online.*/\1/p')
PLAYER_LIST=$(printf '%s\n' "$RCON_OUTPUT" | sed -n 's/^There are [0-9][0-9]* of a max of [0-9][0-9]* players online: \(.*\)$/\1/p')

if [ -z "$PLAYER_COUNT" ]; then
    echo "ERROR: Could not parse RCON output" >&2
    exit 1
fi

if [ "$PLAYER_COUNT" -eq 0 ]; then
    PLAYERS_JSON='[]'
else
    PLAYERS_JSON=$(printf '%s\n' "$PLAYER_LIST" | tr ',' '\n' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//' | jq -R -s 'split("\n") | map(select(length > 0))')
fi

if [ "$PLAYER_COUNT" -eq 0 ] && [ -f "$IDLE_SINCE_FILE" ]; then
    IDLE_SINCE=$(cat "$IDLE_SINCE_FILE")
else
    IDLE_SINCE="null"
fi

jq -n \
    --argjson players "$PLAYERS_JSON" \
    --argjson idleSince "$IDLE_SINCE" \
    '{
        running: true,
        players: $players,
        idle: ($players | length == 0),
        idleSince: $idleSince
    }'
