#!/bin/bash

set -eu

cd /opt/minecraft/
git pull

rm -f /opt/minecraft/server/state/*

cd server
docker compose up -d
