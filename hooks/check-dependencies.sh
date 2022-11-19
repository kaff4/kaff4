#!/bin/sh
set -e

./gradlew buildHealth

BUILD_HEALTH_REPORT="./build/reports/dependency-analysis/build-health-report.txt"

if [ -s $BUILD_HEALTH_REPORT ]; then
  echo "Failed: ./gradlew buildHealth"
  echo ""
  cat $BUILD_HEALTH_REPORT
  exit 1
fi