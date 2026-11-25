#!/bin/sh
# Assemble the archive and compare its HEX and flat binary representations.
set -eu
cd "$(dirname "$0")/.."
build_dir=$(mktemp -d "${TMPDIR:-/tmp}/theradome-build.XXXXXX")
gpasm -o "$build_dir/app.hex" model/rebuild.asm
echo "Assembled image and listing are in $build_dir"
python3 model/tools/rebuild.py verify "$build_dir/app.hex" --binary outputs/myTheradome-LH80-labeled.bin
