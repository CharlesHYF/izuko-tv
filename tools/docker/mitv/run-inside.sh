#!/usr/bin/env bash
# 复制源码并执行容器内的兼容构建任务
# 创建日期：2026-10-22
# 修改日期：2026-10-22
set -euo pipefail

tar -C /source --exclude=.git --exclude=build --exclude=.gradle --exclude=.kotlin \
    --exclude=local.properties --exclude=.build.lock --exclude=artifacts \
    --exclude=.env --exclude='*.keystore' --exclude='*.jks' \
    --exclude=app/desktop/appResources --exclude=.readme \
    -cf - . | tar -C /workspace -xf -

printf '%s\n' 'sdk.dir=/opt/android-sdk' 'jvm.toolchain.vendor=adoptium' \
    'ani.enable.ios=false' 'ani.enable.firebase=false' > /workspace/local.properties

if [ "$#" -eq 0 ]; then
    set -- :app:android:assembleDefaultTvDebug
fi

set +e
bash /workspace/scripts/gradle-lock.sh "$@" \
    -Pani.android.mitv=true -Pani.android.abis=armeabi-v7a \
    -Pani.repository=CharlesHYF/izuko-tv \
    -Dorg.gradle.jvmargs='-Xmx3g -Dfile.encoding=UTF-8' \
    -Pkotlin.daemon.jvmargs=-Xmx1536m --max-workers=2 --console=plain 2>&1 | tee /out/build.log
build_status=${PIPESTATUS[0]}
set -e

if [ "$build_status" -eq 0 ] && [[ "$*" == *assemble* ]] && [ -d /workspace/app/android/build/outputs/apk ]; then
    cp -R /workspace/app/android/build/outputs/apk /out/
fi
exit "$build_status"
