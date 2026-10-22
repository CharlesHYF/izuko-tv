#!/usr/bin/env bash
# 使用任务专属测试密钥签名优化构建并验证 Android 6.0 签名
# 创建日期：2026-10-22
# 修改日期：2026-10-22
set -euo pipefail
mkdir -p /key /signed
if [ ! -f /key/mitv-test.jks ]; then
    keytool -genkeypair -keystore /key/mitv-test.jks -storepass android \
        -keypass android -alias mitv-test -keyalg RSA -keysize 2048 \
        -validity 10000 -dname 'CN=MiTV Compatibility Test'
fi
abi="${MITV_SIGN_ABI:-armeabi-v7a}"
mapfile -t packages < <(find /unsigned -type f -name "*-$abi-release-unsigned.apk")
test "${#packages[@]}" -eq 1
signer="$ANDROID_HOME/build-tools/36.0.0/apksigner"
"$signer" sign --ks /key/mitv-test.jks --ks-pass pass:android \
    --key-pass pass:android --ks-key-alias mitv-test --min-sdk-version 23 \
    --v1-signing-enabled true --out "/signed/izuko-tv-mitv-api23-$abi-release.apk" "${packages[0]}"
"$signer" verify --min-sdk-version 23 --verbose "/signed/izuko-tv-mitv-api23-$abi-release.apk"
