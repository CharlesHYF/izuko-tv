#!/usr/bin/env bash
# 在专属 Linux 容器中启动 Android 6.0 TV 并验证兼容包
# 创建日期：2026-10-22
# 修改日期：2026-10-22
set -euo pipefail

export ANDROID_SERIAL=emulator-5554
export ANDROID_AVD_HOME=/tmp/mitv-avd
export PATH="$ANDROID_HOME/emulator:$PATH"
mkdir -p "$ANDROID_AVD_HOME" /out
exec > >(tee -a /out/verification.log) 2>&1

if [ ! -r /dev/kvm ] || [ ! -w /dev/kvm ]; then
    printf 'API 23 emulator requires readable and writable /dev/kvm\n' >&2
    exit 1
fi

apt-get update -qq
apt-get install -y --no-install-recommends libnss3 libpulse0 libglu1-mesa libxkbcommon0 libvulkan1 >/out/emulator-dependencies.log

set +e
yes | sdkmanager --licenses >/dev/null
license_status=${PIPESTATUS[1]}
set -e
test "$license_status" -eq 0
sdkmanager 'emulator' 'system-images;android-23;android-tv;x86' >/out/sdk-install.log
avdmanager create avd --force --name mitv-api23 \
    --package 'system-images;android-23;android-tv;x86' --device tv_720p <<<'no'

avd_config="$ANDROID_AVD_HOME/mitv-api23.avd/config.ini"
sed -i '/^hw.ramSize=/d; /^vm.heapSize=/d; /^hw.lcd.density=/d' "$avd_config"
printf '%s\n' 'hw.ramSize=1024' 'vm.heapSize=256' 'hw.lcd.density=213' >> "$avd_config"

emulator -avd mitv-api23 -port 5554 -no-window -no-audio -no-boot-anim \
    -no-snapshot -gpu swiftshader_indirect -accel on -memory 1024 -cores 2 \
    >/out/emulator.log 2>&1 &
emulator_pid=$!
trap 'kill "$emulator_pid" 2>/dev/null || true' EXIT

timeout 90 adb wait-for-device
boot_deadline=$((SECONDS + 180))
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; do
    kill -0 "$emulator_pid"
    if [ "$SECONDS" -gt "$boot_deadline" ]; then
        printf 'API 23 emulator boot timed out\n' >&2
        exit 1
    fi
    sleep 2
done

apk="$(find /apks -name '*-x86-debug.apk' -print -quit)"
test -n "$apk"
"$ANDROID_HOME/build-tools/36.0.0/aapt2" dump badging "$apk" >/out/apk-badging.txt
grep -qFx "minSdkVersion:'23'" /out/apk-badging.txt
grep -qFx "native-code: 'x86'" /out/apk-badging.txt
adb install -r -g "$apk"
python3 /source/tools/docker/mitv/emulator-smoke.py
