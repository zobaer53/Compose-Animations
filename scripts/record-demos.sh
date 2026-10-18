#!/usr/bin/env bash
#
# Records every demo on a connected phone and turns the clips into the README's GIFs, then renders
# the README cover. Everything lands in docs/media/.
#
#   scripts/record-demos.sh                 # cover + every demo
#   scripts/record-demos.sh dailycheckin    # just these demos (no cover)
#   ANDROID_SERIAL=<serial> scripts/record-demos.sh   # pick a phone when several are connected
#
# Needs adb and ffmpeg. It never starts an emulator: connect a real phone. Before every tap or
# keystroke it checks that this app has focus, so input can't land in another app.
#
# Adding a demo: add its slug to DEMOS, its list title to title_of, and a scenario_<slug>
# function that looks up its tap targets, calls start_recording and then plays the demo.

set -euo pipefail

PKG=com.zobaer53.composeanimations
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/docs/media"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# GIF settings: small enough for a README, smooth enough to show the motion.
GIF_WIDTH=320
GIF_FPS=15

DEMOS=(forgetpassword dailycheckin)

title_of() {
  case "$1" in
    forgetpassword) echo "Forget password" ;;
    dailycheckin) echo "Daily check-in" ;;
    *) die "unknown demo '$1' (known: ${DEMOS[*]})" ;;
  esac
}

die() { echo "record-demos: $*" >&2; exit 1; }

# --- Device -----------------------------------------------------------------------------------

pick_device() {
  if [[ -n "${ANDROID_SERIAL:-}" ]]; then
    SERIAL="$ANDROID_SERIAL"
  else
    local devices
    devices=$(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')
    case $(printf '%s' "$devices" | grep -c .) in
      0) die "no phone connected. Connect one with USB debugging on (this script never starts an emulator)." ;;
      1) SERIAL="$devices" ;;
      *) die "several devices connected; set ANDROID_SERIAL to one of: $(echo $devices)" ;;
    esac
  fi
  ADB=(adb -s "$SERIAL")
  echo "Recording on $("${ADB[@]}" shell getprop ro.product.model | tr -d '\r') ($SERIAL)"
}

app_has_focus() {
  "${ADB[@]}" shell "dumpsys window 2>/dev/null" | grep -q "mCurrentFocus=.*$PKG/"
}

wait_for_focus() {
  for _ in $(seq 1 20); do
    app_has_focus && return
    sleep 0.5
  done
  die "$PKG never got focus (is something else on screen?)"
}

# Sends `adb shell input <args>`, but only if this app has focus. The check and the input run in
# one shell on the phone, so nothing can steal focus in between.
app_input() {
  "${ADB[@]}" shell "dumpsys window 2>/dev/null | grep -q 'mCurrentFocus=.*$PKG/' && input $*" \
    || die "$PKG lost focus; not sending 'input $*'"
}

# Prints "x y", the centre of the first view whose <attribute> is <value>, from a fresh UI dump.
#   center_of text "Reset"      center_of content-desc "Water Sprout"
#   center_of class android.widget.EditText
center_of() {
  "${ADB[@]}" shell uiautomator dump /sdcard/record-demos.xml >/dev/null
  local point
  point=$("${ADB[@]}" shell cat /sdcard/record-demos.xml \
    | grep -o "$1=\"$2\"[^>]*bounds=\"[^\"]*\"" | head -1 \
    | grep -oE '\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]' | tr '[],' '   ' \
    | awk '{ print int(($1 + $3) / 2), int(($2 + $4) / 2) }') || true
  [[ -n "$point" ]] || die "nothing on screen with $1=\"$2\""
  echo "$point"
}

# The app is edge to edge; crop the status and navigation bars out of the recordings.
read_screen_geometry() {
  SCREEN_H=$("${ADB[@]}" shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1 | cut -dx -f2)
  local window
  window=$("${ADB[@]}" shell dumpsys window)
  STATUS_BOTTOM=$(grep -oE 'type=statusBars[^}]*frame=\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]' <<<"$window" \
    | head -1 | grep -oE '[0-9]+\]$' | tr -d ']' || true)
  NAV_TOP=$(grep -oE 'type=navigationBars[^}]*frame=\[[0-9]+,[0-9]+\]' <<<"$window" \
    | head -1 | grep -oE '[0-9]+\]$' | tr -d ']' || true)
  STATUS_BOTTOM=${STATUS_BOTTOM:-0}
  NAV_TOP=${NAV_TOP:-$SCREEN_H}
}

# For a scenario that types: stop the GIF at the keyboard's top edge, so it shows the demo, not
# half a screen of keys. Call it once the keyboard is up. (Hiding the keyboard doesn't work:
# it pops back up as soon as text arrives.)
crop_above_keyboard() {
  local top
  top=$("${ADB[@]}" shell "dumpsys window 2>/dev/null" | grep -oE 'type=ime frame=\[[0-9]+,[0-9]+\][^}]*visible=true' \
    | head -1 | grep -oE 'frame=\[[0-9]+,[0-9]+' | cut -d, -f2 || true)
  [[ -n "$top" ]] || die "expected the keyboard to be up"
  CROP_BOTTOM=$top
}

# --- Recording --------------------------------------------------------------------------------

start_recording() {
  "${ADB[@]}" shell rm -f /sdcard/record-demos.mp4
  "${ADB[@]}" shell screenrecord --bit-rate 12000000 /sdcard/record-demos.mp4 &
  RECORDER=$!
  sleep 1
}

stop_recording() {
  "${ADB[@]}" shell pkill -INT screenrecord || true
  wait "$RECORDER" 2>/dev/null || true
  sleep 1
  "${ADB[@]}" pull /sdcard/record-demos.mp4 "$WORK/$1.mp4" >/dev/null
  "${ADB[@]}" shell rm -f /sdcard/record-demos.mp4
}

to_gif() {
  # The recording may be scaled down from the screen, so crop by fraction of its height.
  local crop="crop=iw:ih*($CROP_BOTTOM-$STATUS_BOTTOM)/$SCREEN_H:0:ih*$STATUS_BOTTOM/$SCREEN_H"
  ffmpeg -v error -y -i "$1" -vf "$crop,fps=$GIF_FPS,scale=$GIF_WIDTH:-2:flags=lanczos,split[a][b];\
[a]palettegen=max_colors=128:stats_mode=diff[p];[b][p]paletteuse=dither=bayer:bayer_scale=4:diff_mode=rectangle" "$2"
}

open_demo() {
  "${ADB[@]}" shell am start -S -W -n "$PKG/.MainActivity" >/dev/null
  wait_for_focus
  local x y
  read -r x y <<<"$(center_of text "$1")"
  app_input tap "$x" "$y"
  sleep 1.5
  wait_for_focus
}

# --- Scenarios: each looks up its targets, starts recording, then plays the demo ---------------

scenario_forgetpassword() {
  local x y
  read -r x y <<<"$(center_of class android.widget.EditText)"
  app_input tap "$x" "$y"
  sleep 1.5
  crop_above_keyboard

  start_recording
  sleep 1.5
  # Typed one key at a time so the dog stretches and changes mood step by step.
  for c in $(fold -w1 <<<"longerisstronger"); do
    app_input text "$c"
    sleep 0.12
  done
  # Tongue out, then it settles into a content smile. Ends here: Reset would close the keyboard
  # and move the card out of the cropped area.
  sleep 3.5
}

scenario_dailycheckin() {
  local x y
  read -r x y <<<"$(center_of content-desc "Water Sprout")"

  start_recording
  sleep 2.5 # the thirsty idle: fidgeting, and the sulky cloud
  app_input swipe "$x" "$y" "$x" "$y" 2800
  sleep 5   # bloom, the wave, the cloud floating off, settling
}

# --- Cover ------------------------------------------------------------------------------------

render_cover() {
  "${ADB[@]}" shell run-as "$PKG" rm -f files/cover.png
  "${ADB[@]}" shell am start -S -W -n "$PKG/.cover.CoverActivity" >/dev/null
  for _ in $(seq 1 30); do
    if "${ADB[@]}" shell run-as "$PKG" ls files/cover.png >/dev/null 2>&1; then
      "${ADB[@]}" exec-out run-as "$PKG" cat files/cover.png >"$OUT/cover.png"
      echo "  docs/media/cover.png"
      return
    fi
    sleep 0.5
  done
  die "the cover never rendered"
}

# --- Main -------------------------------------------------------------------------------------

command -v ffmpeg >/dev/null || die "ffmpeg not found"
pick_device
mkdir -p "$OUT"

echo "Installing the debug app"
(cd "$ROOT" && ANDROID_SERIAL="$SERIAL" ./gradlew -q :app:installDebug)
read_screen_geometry

if [[ $# -eq 0 ]]; then
  echo "Rendering the cover"
  render_cover
fi

for slug in "${@:-${DEMOS[@]}}"; do
  title=$(title_of "$slug")
  echo "Recording $title"
  CROP_BOTTOM=$NAV_TOP
  open_demo "$title"
  "scenario_$slug"
  stop_recording "$slug"
  to_gif "$WORK/$slug.mp4" "$OUT/$slug.gif"
  echo "  docs/media/$slug.gif ($(du -h "$OUT/$slug.gif" | cut -f1 | tr -d ' '))"
done
