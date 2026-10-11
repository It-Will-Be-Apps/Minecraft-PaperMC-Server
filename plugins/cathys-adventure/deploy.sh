#!/bin/bash

set -eu

./gradlew build --info
./../../scripts/deploy.sh
./../../scripts/restart.sh
./../../scripts/logs.sh
