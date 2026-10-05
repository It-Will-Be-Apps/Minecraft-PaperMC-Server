#!/bin/bash

set -euo pipefail

COMPOSE_FILE="/opt/minecraft/server/compose.yaml"
SERVICE_NAME="minecraft-server"
WORLD_DATA="/opt/minecraft/data"

if [ "$#" -ne 1 ]; then
    echo "ERROR: Expected temporary EBS volume ID."
    exit 1
fi

MOUNT_POINT="/mnt/minecraft-restore"
TEMP_VOLUME_ID="$1"
TEMP_VOLUME_SERIAL="${TEMP_VOLUME_ID//-/}"

cleanup() {
    if mountpoint -q "$MOUNT_POINT"; then
        echo "Unmounting restore volume..."
        umount "$MOUNT_POINT"
    fi

    rmdir "$MOUNT_POINT" 2>/dev/null || true
}

trap cleanup EXIT

echo "=== Minecraft world restore ==="
echo "Temporary volume: $TEMP_VOLUME_ID"

mkdir -p "$MOUNT_POINT"

# Find the NVMe device belonging to the temporary EBS volume
DEVICE=""
for i in {1..60}; do
    DEVICE=$(lsblk -o NAME,SERIAL,TYPE -nr | awk -v vol="$TEMP_VOLUME_SERIAL" '$2 == vol && $3 == "disk" {print "/dev/" $1; exit}')

    if [ -n "$DEVICE" ]; then
        break
    fi

    sleep 2
done

if [ -z "$DEVICE" ]; then
    echo "ERROR: Could not find device for EBS volume $TEMP_VOLUME_ID"
    exit 1
fi

echo "Temporary restore device: $DEVICE"

# Mount the volume
mount -o ro "$DEVICE" "$MOUNT_POINT"
echo "Restore volume mounted"

# Wipe the world data
echo "Removing existing Minecraft world..."
find "$WORLD_DATA" -mindepth 1 -maxdepth 1 -exec rm -rf -- {} +

# Restore the world from the snapshot
echo "Restoring Minecraft world..."
cp -a "$MOUNT_POINT" "$WORLD_DATA"

echo "Minecraft world restored."
