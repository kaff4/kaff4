#!/bin/sh
set -e

cd ./engine-jvm
./gradlew detekt --continue
cd -