#!/bin/bash

# ----------------------------------
# 1. General setup
# ----------------------------------
set -eux

# This creates a very useful for troubleshooting user data errors, by running cat /var/log/user-data.log on the instance
exec > >(tee /var/log/user-data.log | logger -t user-data -s 2>/dev/console) 2>&1
dnf update -y

# Save the S3 bucket name in an environment variable
export PLUGINS_BUCKET_NAME="__PLUGINS_BUCKET_NAME__"

# ----------------------------------
# 2. Install Docker
# ----------------------------------
dnf install -y docker
systemctl enable --now docker
usermod -aG docker ec2-user

# ----------------------------------
# 3. Install Docker Compose
# ----------------------------------
mkdir -p /usr/libexec/docker/cli-plugins
curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-$(uname -m) -o /usr/libexec/docker/cli-plugins/docker-compose
chmod +x /usr/libexec/docker/cli-plugins/docker-compose

# ----------------------------------
# 4. Create the directory
# ----------------------------------
mkdir -p /opt/minecraft
chown -R ec2-user:ec2-user /opt/minecraft

# ----------------------------------
# 5. Install Git
# ----------------------------------
dnf install -y git

# ----------------------------------
# 6. Clone the repository
# ----------------------------------
cd /opt/minecraft
git clone __GIT_HUB_REPOSITORY_URL__ .
git config --system --add safe.directory /opt/minecraft

# ----------------------------------
# 7. Enable the SSM agent
# ----------------------------------
systemctl enable amazon-ssm-agent
systemctl start amazon-ssm-agent

# ----------------------------------
# 8. Prepare the world data volume
# ----------------------------------
WORLD_MOUNT="/opt/minecraft/data"
WORLD_VOLUME_ID="__WORLD_VOLUME_ID__"
WORLD_VOLUME_SERIAL="${WORLD_VOLUME_ID//-/}"

mkdir -p "$WORLD_MOUNT"

# Wait for the EBS volume to be attached
WORLD_DEVICE=""
for i in {1..60}; do
    WORLD_DEVICE=$(lsblk -o NAME,SERIAL,TYPE -nr | awk -v vol="$WORLD_VOLUME_SERIAL" '$2 == vol && $3 == "disk" {print "/dev/" $1; exit}')

    if [ -n "$WORLD_DEVICE" ]; then
        break
    fi

    sleep 2
done

if [ -z "$WORLD_DEVICE" ]; then
    echo "ERROR: Could not find world EBS volume $WORLD_VOLUME_SERIAL"
    exit 1
fi

echo "World EBS volume found at $WORLD_DEVICE"

# Format the volume, only if it has no filesystem
if ! blkid "$WORLD_DEVICE" >/dev/null 2>&1; then
    echo "World volume has no filesystem. Formatting as XFS..."
    mkfs -t xfs "$WORLD_DEVICE"
else
    echo "World volume already has a filesystem. NOT formatting."
fi

# Get UUID and configure persistent mount
WORLD_UUID=$(blkid -s UUID -o value "$WORLD_DEVICE")

if ! grep -q "UUID=$WORLD_UUID $WORLD_MOUNT " /etc/fstab; then
    echo "UUID=$WORLD_UUID $WORLD_MOUNT xfs defaults,nofail 0 2" >> /etc/fstab
fi

if ! mountpoint -q "$WORLD_MOUNT"; then
    mount "$WORLD_MOUNT"
fi

echo "World volume mounted:"
df -h "$WORLD_MOUNT"

# ----------------------------------
# 9. Create the deployment service
# ----------------------------------
cat > /etc/systemd/system/minecraft-deploy.service <<'EOF'
[Unit]
Description=Deploy and start Minecraft server
After=docker.service
Requires=docker.service
RequiresMountsFor=/opt/minecraft/data

[Service]
Type=oneshot
ExecStart=/opt/minecraft/server/scripts/deploy.sh
RemainAfterExit=yes

[Install]
WantedBy=multi-user.target
EOF

# ----------------------------------
# 10. Create the watchdog service
# ----------------------------------
cat > /etc/systemd/system/minecraft-watchdog.service <<'EOF'
[Unit]
Description=Track active players to know when to shut down the instance
After=docker.service
Requires=docker.service

[Service]
Type=simple
ExecStart=/opt/minecraft/server/scripts/watchdog.sh
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

# ----------------------------------
# 11. Enable the services
# ----------------------------------
systemctl daemon-reload
systemctl enable --now minecraft-deploy.service
systemctl enable --now minecraft-watchdog.service

echo "User data script successfully completed"
