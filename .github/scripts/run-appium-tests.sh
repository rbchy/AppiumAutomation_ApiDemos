#!/usr/bin/env bash
# Runs inside the Android emulator step: starts Appium, waits for it, then runs one TestNG suite.
# Usage: run-appium-tests.sh <suite.xml> <report-name>
set -uo pipefail
SUITE="$1"; NAME="$2"

adb devices
# boot_completed can be reported before system services are up; wait until they answer
for i in $(seq 1 90); do
  if adb shell settings get global device_provisioned 2>/dev/null | grep -q 1 \
     && adb shell pm path android 2>/dev/null | grep -q package; then
    echo "Android services ready after $((i*2)) s"; break
  fi
  sleep 2
done
adb shell input keyevent 82 || true   # dismiss the lock screen if shown
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

mkdir -p target/ci
nohup appium --address 127.0.0.1 --port 4723 --log-level warn > "target/ci/appium-$NAME.log" 2>&1 &
APPIUM_PID=$!
for i in $(seq 1 60); do
  curl -sf http://127.0.0.1:4723/status >/dev/null && break
  sleep 2
done
curl -sf http://127.0.0.1:4723/status >/dev/null || { echo "Appium did not start"; cat "target/ci/appium-$NAME.log"; exit 1; }

mvn -B --no-transfer-progress test -DsuiteXmlFile="$SUITE"
RESULT=$?

mkdir -p "target/ci/$NAME"
cp -r target/surefire-reports "target/ci/$NAME/" 2>/dev/null || true
kill "$APPIUM_PID" 2>/dev/null || true
exit $RESULT
