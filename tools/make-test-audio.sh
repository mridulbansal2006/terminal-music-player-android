#!/usr/bin/env bash
# Generates small tagged MP3s for demoing TermTune on an emulator.
#
#   ./tools/make-test-audio.sh [output-dir]
#
# Requires ffmpeg with libmp3lame. The tracks are plain sine tones, so they are
# tiny and carry no third-party audio. Push them to a device with:
#
#   adb push <output-dir>/*.mp3 /sdcard/Music/
#   adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE \
#       -d file:///sdcard/Music/<file>.mp3
set -euo pipefail

OUT_DIR="${1:-build/test-audio}"
mkdir -p "$OUT_DIR"

generate() {
  local file="$1" freq="$2" seconds="$3" title="$4" artist="$5" album="$6"
  ffmpeg -y -loglevel error \
    -f lavfi -i "sine=frequency=${freq}:duration=${seconds}" \
    -metadata title="$title" \
    -metadata artist="$artist" \
    -metadata album="$album" \
    -codec:a libmp3lame -b:a 128k \
    "$OUT_DIR/$file"
  echo "  wrote $OUT_DIR/$file  (${seconds}s, ${freq}Hz)"
}

echo "Generating test audio in $OUT_DIR"
generate "01-kernel-panic-blues.mp3"  440 12 "Kernel Panic Blues"   "The Daemons"    "Segfault Sessions"
generate "02-null-pointer-waltz.mp3"  523 18 "Null Pointer Waltz"   "Stack Overflow" "Segfault Sessions"
generate "03-fork-bomb-anthem.mp3"    659 25 "Fork Bomb Anthem"     "The Daemons"    "Root Access"
generate "04-quiet-exit-status.mp3"   349 31 "Quiet Exit Status"    "Zsh Collective" "Root Access"
echo "Done."
