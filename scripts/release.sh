#!/usr/bin/env bash
# Builds SimpleTV for every household and posts the builds as one release on the private
# builds repo. Each stick checks for a new release every 6 hours and installs its own
# household's build (Updater.kt).
#
#   scripts/release.sh
#
# ~/.config/simpletv itself is our household, "home". Each subfolder with a home.txt (and
# its own sync_key) is another household, built with -Phousehold=<folder>. Every build needs
# ~/.config/simpletv/update_token, a read-only token for the builds repo.
set -euo pipefail
cd "$(dirname "$0")/.."

repo=dbarkman/SimpleTV-builds
config="$HOME/.config/simpletv"
export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"

if [[ ! -s "$config/update_token" ]]; then
  echo "No $config/update_token: sticks couldn't fetch this release." >&2
  exit 1
fi

households=()
for dir in "$config"/*/; do
  [[ -f "$dir/home.txt" ]] && households+=("$(basename "$dir")")
done
# Ours last, so the APK left in app/build (which setup-stick.sh installs) is ours.
households+=(home)

version=$(( $(date +%s) / 60 ))
out="$(mktemp -d -t simpletv-release)"
trap 'rm -rf "$out"' EXIT

./gradlew -q testDebugUnitTest

printf '# household versionCode sha256 asset\n' > "$out/latest.txt"
for household in "${households[@]}"; do
  echo "Building $household ($version)"
  property=()
  [[ $household != home ]] && property=("-Phousehold=$household")
  ./gradlew -q assembleDebug "-PversionCode=$version" ${property[@]+"${property[@]}"}
  apk="$out/simpletv-$household.apk"
  cp app/build/outputs/apk/debug/app-debug.apk "$apk"
  sum="$(shasum -a 256 "$apk" | cut -d' ' -f1)"
  printf '%s %s %s %s\n' "$household" "$version" "$sum" "$(basename "$apk")" >> "$out/latest.txt"
done

notes="$(git log -1 --format='%h %s')"
gh release create "build-$version" --repo "$repo" --title "SimpleTV $version" --notes "$notes" \
  "$out/latest.txt" "$out"/simpletv-*.apk
echo "Released $version for: ${households[*]}"
