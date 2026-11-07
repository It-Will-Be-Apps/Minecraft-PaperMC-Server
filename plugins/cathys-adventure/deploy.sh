#!/bin/bash

set -eu

./gradlew clean build buildResourcePack --info

cp build/libs/* /data/plugins/

./../../scripts/restart.sh
./../../scripts/logs.sh
