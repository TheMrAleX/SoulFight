#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
version="${1:?Usage: scripts/release.sh 0.1.1}"
if [[ -f /workspace/tools/jdk-17.0.21+9/bin/java ]]; then source scripts/environment.sh; fi
signing_dir="${FIGHTFORGE_SIGNING_DIR:-/workspace/fightforge-signing}"
test -s "$signing_dir/release.p12"
test -s "$signing_dir/store-password"
test -z "$(git status --porcelain)"
code="$(git rev-list --count HEAD)"
./gradlew test assembleDebug assembleRelease -PreleaseName="$version" -PreleaseCode="$code" --max-workers=4
"${ANDROID_HOME:?}/build-tools/34.0.0/apksigner" verify --verbose app/build/outputs/apk/release/app-release.apk
mkdir -p /tmp/fightforge-release
cp app/build/outputs/apk/release/app-release.apk /tmp/fightforge-release/FightForge.apk
(cd /tmp/fightforge-release && sha256sum FightForge.apk > SHA256SUMS)
git push origin HEAD:main
gh release create "v$version" /tmp/fightforge-release/FightForge.apk /tmp/fightforge-release/SHA256SUMS --repo TheMrAleX/SoulFight --target "$(git rev-parse HEAD)" --title "Fight Forge $version" --prerelease --notes-file RELEASE_NOTES.md
