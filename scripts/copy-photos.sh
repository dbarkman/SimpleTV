#!/usr/bin/env bash
# Copies the family photos from one stick's SimpleTV to another's, adding to what's
# there. The photos pass through this computer in a temporary file that's deleted
# afterwards. Needs a debug build on both sticks (it uses run-as).
#
#   scripts/copy-photos.sh <from-stick-ip> <to-stick-ip>
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "usage: $0 <from-stick-ip> <to-stick-ip>" >&2
  exit 1
fi

from="$1:5555"
to="$2:5555"
app=com.redmesastudios.simpletv

bundle="$(mktemp -t simpletv-photos)"
trap 'rm -f "$bundle"' EXIT

adb connect "$from" < /dev/null
adb connect "$to" < /dev/null

adb -s "$from" exec-out run-as "$app" tar -c -C files photos < /dev/null > "$bundle"
adb -s "$to" exec-in run-as "$app" tar -x -C files < "$bundle"

echo "From: $(adb -s "$from" shell run-as "$app" ls files/photos < /dev/null | wc -l | tr -d ' ') files"
echo "To:   $(adb -s "$to" shell run-as "$app" ls files/photos < /dev/null | wc -l | tr -d ' ') files"
