#!/bin/bash

set -eu

./gradlew clean build --info

cp build/libs/* /data/plugins/

./../../scripts/stop.sh
yes | ./../../scripts/wipe.sh
./../../scripts/start.sh
./../../scripts/logs.sh
