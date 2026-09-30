#!/bin/bash

set -eu

cd /opt/minecraft/
git pull

cd server
docker compose up -d
