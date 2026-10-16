#!/bin/bash

set -eu

cp build/libs/* /data/plugins/

./gradlew build --info

./../../scripts/stop.sh
yes | ./../../scripts/wipe.sh
./../../scripts/start.sh
./../../scripts/logs.sh
