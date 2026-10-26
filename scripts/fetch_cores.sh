#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# AETHONE – core fetcher
#
# Downloads the Aether (WARP/MASQUE) and Zeptun (tun2socks) Android binaries
# from their GitHub releases and installs them under app/src/main/jniLibs as
# lib*.so files, the only naming Android's package manager extracts and marks
# executable.
#
# The Gradle task `fetchCores` does the same thing automatically before the
# first build – this script exists for CI, containers and plain curl lovers.
#
# Usage:
#   sh scripts/fetch_cores.sh            # download if missing
#   FORCE=1 sh scripts/fetch_cores.sh    # re-download
# ---------------------------------------------------------------------------
set -euo pipefail

cd "$(dirname "$0")/.."
ROOT=$(pwd)
JNI="$ROOT/app/src/main/jniLibs"

AETHER_VERSION="${AETHER_VERSION:-v2.4.0}"
ZEPTUN_VERSION="${ZEPTUN_VERSION:-v1.1.3}"
FORCE="${FORCE:-0}"

AETHER_BASE="https://github.com/CluvexStudio/Aether/releases/download"
ZEPTUN_URL="https://github.com/Noisemux/zeptun/releases/download/$ZEPTUN_VERSION/zeptun-android-jniLibs.zip"

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

fetch() { # fetch <url> <dest>
    echo "· downloading $1"
    curl -fSL --retry 3 -o "$2" "$1"
}

aether_present() { # aether_present <abi>
    [ -f "$JNI/$1/libaether.so" ]
}

zeptun_present() { # zeptun_present <abi>
    [ -f "$JNI/$1/libzeptun-jni.so" ]
}

# Aether ships: arm64, armv7, x86_64 (no 32-bit x86)
install_aether() {
    local abi="$1" arch="$2"
    if [ "$FORCE" != "1" ] && aether_present "$abi"; then
        echo "· aether: $abi already present, skipping"
        return
    fi
    fetch "$AETHER_BASE/$AETHER_VERSION/aether-android-$arch.tar.gz" "$TMP/aether-$arch.tar.gz"
    mkdir -p "$JNI/$abi"
    tar -xzf "$TMP/aether-$arch.tar.gz" -C "$TMP"
    cp "$TMP/aether"                      "$JNI/$abi/libaether.so"
    mkdir -p "$TMP/pt-out"
    if [ -d "$TMP/pt" ]; then
        cp "$TMP/pt/lyrebird"             "$JNI/$abi/liblyrebird.so" 2>/dev/null || true
        cp "$TMP/pt/psiphon-tunnel-core"  "$JNI/$abi/libpsiphon.so" 2>/dev/null || true
    fi
    rm -rf "$TMP/pt" "$TMP/aether"
    echo "✓ aether $AETHER_VERSION -> $abi"
}

install_zeptun() {
    local all_present=1
    for abi in arm64-v8a armeabi-v7a x86_64; do
        if ! zeptun_present "$abi"; then all_present=0; fi
    done
    if [ "$FORCE" != "1" ] && [ "$all_present" = "1" ]; then
        echo "· zeptun: all ABIs already present, skipping"
        return
    fi
    fetch "$ZEPTUN_URL" "$TMP/zeptun.zip"
    unzip -oq "$TMP/zeptun.zip" -d "$TMP/zeptun"
    for abi in arm64-v8a armeabi-v7a x86_64; do
        if [ -d "$TMP/zeptun/jniLibs/$abi" ]; then
            mkdir -p "$JNI/$abi"
            cp "$TMP/zeptun/jniLibs/$abi/"*.so "$JNI/$abi/"
            echo "✓ zeptun $ZEPTUN_VERSION -> $abi"
        fi
    done
}

echo "AETHONE core fetcher"
install_aether arm64-v8a arm64
install_aether armeabi-v7a armv7
install_aether x86_64 x86_64
install_zeptun

echo
echo "Done. Files installed under app/src/main/jniLibs:"
ls -1 "$JNI"/*/ | sed 's/^/  /'
