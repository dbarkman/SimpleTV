#!/usr/bin/env bash
# Sets up an onn 4K stick for SimpleTV over adb: installs the app, makes it the home
# screen and turns on its extras. Safe to run again.
#
#   scripts/setup-stick.sh <stick-ip>
#
# Before running it, on the stick: finish Google TV setup, turn on Developer options
# and Network debugging, and set the sleep timer to Never. Build the APK first
# (./gradlew assembleDebug). The first connection asks "Allow network debugging?"
# on the TV; tick Always allow.
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: $0 <stick-ip>" >&2
  exit 1
fi

serial="$1:5555"
app=com.redmesastudios.simpletv
apk="$(dirname "$0")/../app/build/outputs/apk/debug/app-debug.apk"

# The stock home screen and its setup companion step aside for SimpleTV.
home_off=(com.google.android.apps.tv.launcherx com.google.android.tungsten.setupwraith)
# Apps the stock home pushed that SimpleTV leaves out. Optional; edit to taste.
extras_off=(com.google.android.play.games com.google.android.youtube.tvmusic)

if [[ ! -f "$apk" ]]; then
  echo "No APK at $apk. Build it first: ./gradlew assembleDebug" >&2
  exit 1
fi

adb connect "$serial" < /dev/null
echo "Waiting for the stick. Approve \"Allow network debugging?\" on the TV if it asks."
until [[ "$(adb -s "$serial" get-state 2> /dev/null < /dev/null)" == device ]]; do
  sleep 2
done

on() { adb -s "$serial" shell "$@" < /dev/null; }

echo "Stick: $(on getprop ro.product.model), Android $(on getprop ro.build.version.release)"

echo "Installing SimpleTV"
adb -s "$serial" install -r "$apk" < /dev/null

echo "Turning on the remote buttons, usage access, voice search and the photo screensaver"
on settings put secure enabled_accessibility_services "$app/.RemoteButtons"
on settings put secure accessibility_enabled 1
on appops set "$app" GET_USAGE_STATS allow
on pm grant "$app" android.permission.RECORD_AUDIO
on settings put secure screensaver_components "$app/.PhotoDream"

# Last, because the stick can drop off Wi-Fi when its home screen changes. If it does,
# wake it and run this script again; every step is safe to repeat.
echo "Making SimpleTV the home screen"
installed="$(on pm list packages)"
for pkg in "${extras_off[@]}" "${home_off[@]}"; do
  if grep -qx "package:$pkg" <<< "$installed"; then
    on pm disable-user --user 0 "$pkg"
  fi
done
on input keyevent KEYCODE_HOME

echo
echo "Done. Remote buttons: $(on settings get secure enabled_accessibility_services)"
echo "Disabled: $(on pm list packages -d | tr '\n' ' ')"
