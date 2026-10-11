#!/bin/bash

set -eu

./gradlew build --info
./../../scripts/deploy.sh
./../../scripts/stop.sh
yes | ./../../scripts/wipe.sh
./../../scripts/start.sh
./../../scripts/logs.sh
