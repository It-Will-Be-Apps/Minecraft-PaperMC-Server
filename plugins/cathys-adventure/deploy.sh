#!/bin/bash

set -eu

cp build/libs/* /data/plugins/

./gradlew build --info

./../../scripts/restart.sh
./../../scripts/logs.sh
