#!/bin/bash

set -eu

COMPOSE_FILE="/opt/minecraft/server/compose.yaml"
SERVICE_NAME="minecraft-server"
STATE_DIR="/opt/minecraft/server/state"
IDLE_SINCE_FILE="$STATE_DIR/idle-since"

# How often to reconcile with RCON even if no log event occurs.
RECONCILE_INTERVAL_SECONDS=300

mkdir -p "$STATE_DIR"

log() {
    echo "[$(date -u '+%Y-%m-%dT%H:%M:%SZ')] $*"
}

get_container_id() {
    docker compose \
        -f "$COMPOSE_FILE" \
        ps -q "$SERVICE_NAME"
}

is_container_running() {
    local container_id="$1"

    [ "$(docker inspect -f '{{.State.Running}}' "$container_id" 2>/dev/null || echo false)" = "true" ]
}

get_player_count() {
    local container_id="$1"
    local output

    output=$(docker compose \
        -f "$COMPOSE_FILE" \
        exec -T "$SERVICE_NAME" \
        rcon-cli list)

    if [[ "$output" =~ There\ are\ ([0-9]+)\ of\ a\ max\ of ]]; then
        echo "${BASH_REMATCH[1]}"
        return 0
    fi

    log "ERROR: Could not parse RCON output: $output"
    return 1
}

set_idle_since() {
    local timestamp="$1"
    local temp_file

    temp_file=$(mktemp "$STATE_DIR/idle-since.XXXXXX")

    printf '%s\n' "$timestamp" > "$temp_file"
    mv "$temp_file" "$IDLE_SINCE_FILE"
}

clear_idle_since() {
    rm -f "$IDLE_SINCE_FILE"
}

reconcile() {
    local container_id
    local player_count
    local now

    container_id=$(get_container_id)

    if [ -z "$container_id" ]; then
        log "Minecraft container does not exist"
        clear_idle_since
        return
    fi

    if ! is_container_running "$container_id"; then
        log "Minecraft container is not running"
        clear_idle_since
        return
    fi

    if ! player_count=$(get_player_count "$container_id"); then
        log "RCON reconciliation failed; leaving idle state unchanged"
        return
    fi

    now=$(date -u '+%Y-%m-%dT%H:%M:%SZ')

    if [ "$player_count" -eq 0 ]; then
        if [ -f "$IDLE_SINCE_FILE" ]; then
            log "Minecraft is still idle"
        else
            set_idle_since "$now"
            log "Minecraft became idle at $now"
        fi
    else
        if [ -f "$IDLE_SINCE_FILE" ]; then
            log "Minecraft is no longer idle ($player_count player(s) online)"
            clear_idle_since
        else
            log "Minecraft has $player_count player(s) online"
        fi
    fi
}

handle_log_event() {
    local line="$1"

    case "$line" in
        *"joined the game"*)
            log "Detected player join event"
            reconcile
            ;;

        *"left the game"*)
            log "Detected player leave event"
            reconcile
            ;;
    esac
}

watch_logs() {
    while true; do
        local container_id

        container_id=$(get_container_id)

        if [ -z "$container_id" ] || ! is_container_running "$container_id"; then
            sleep 5
            continue
        fi

        log "Starting Minecraft log watcher"

        docker compose \
            -f "$COMPOSE_FILE" \
            logs \
            --follow \
            --no-log-prefix \
            "$SERVICE_NAME" 2>/dev/null |
        while IFS= read -r line; do
            handle_log_event "$line"
        done

        log "Minecraft log watcher stopped; restarting..."
        sleep 2
    done
}

cleanup() {
    log "Stopping idle watcher"
    jobs -p | xargs -r kill 2>/dev/null || true
}

trap cleanup EXIT INT TERM

log "Minecraft idle watcher starting"

# Establish the initial state.
reconcile

# Watch Minecraft logs in the background.
watch_logs &

# Periodically reconcile in case a log event was missed.
while true; do
    sleep "$RECONCILE_INTERVAL_SECONDS"
    reconcile
done