#!/bin/bash

# ----------------------------------
# 1. General setup
# ----------------------------------
set -eux
# This creates a very useful for troubleshooting user data errors, by running cat /var/log/user-data.log on the instance
exec > >(tee /var/log/user-data.log | logger -t user-data -s 2>/dev/console) 2>&1
dnf update -y

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
git clone https://github.com/It-Will-Be-Apps/Minecraft-PaperMC-Server.git .
git config --system --add safe.directory /opt/minecraft

# ----------------------------------
# 7. Retrieve the secrets
# ----------------------------------
cd /opt/minecraft/server
touch .env.secrets

# ----------------------------------
# 8. Enable the SSM agent
# ----------------------------------
systemctl enable amazon-ssm-agent
systemctl start amazon-ssm-agent

# ----------------------------------
# 9. Create the deployment service
# ----------------------------------
cat > /etc/systemd/system/minecraft-deploy.service <<'EOF'
[Unit]
Description=Deploy and start Minecraft server
After=docker.service
Requires=docker.service

[Service]
Type=oneshot
ExecStart=/opt/minecraft/server/scripts/deploy.sh
RemainAfterExit=yes

[Install]
WantedBy=multi-user.target
EOF

# ----------------------------------
# 10. Enable the deployment service
# ----------------------------------
systemctl daemon-reload
systemctl enable --now minecraft-deploy.service
