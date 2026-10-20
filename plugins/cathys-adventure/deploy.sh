#!/bin/bash

set -eu

./gradlew clean build --info

cp build/libs/* /data/plugins/

./../../scripts/restart.sh
./../../scripts/logs.sh
