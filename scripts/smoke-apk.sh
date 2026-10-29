#!/usr/bin/env bash
set -euo pipefail
apk=app/build/outputs/apk/release/app-release.apk
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --verbose "$apk"
adb install -r "$apk"
adb logcat -c
adb shell am start -W -n dev.sketchlive.android/.MainActivity > launch.txt
cat launch.txt
grep -q 'Status: ok' launch.txt
sleep 5
adb shell pidof dev.sketchlive.android
adb logcat -d -b crash > crash.txt
if grep -q 'Process: dev.sketchlive.android' crash.txt; then
  cat crash.txt
  exit 1
fi
# This checks startup without downloading the multi-GB inference models.
