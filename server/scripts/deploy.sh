#!/bin/bash

set -eux

cd /opt/minecraft/
git pull

cd server
docker compose up -d
