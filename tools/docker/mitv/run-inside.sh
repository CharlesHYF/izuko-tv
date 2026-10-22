#!/usr/bin/env bash
# 复制源码并执行容器内的兼容构建任务
# 创建日期：2026-10-22
# 修改日期：2026-10-22
set -euo pipefail

# 固定容器名保证工作卷独占，容器重启时清理其上一次遗留的构建锁。
rm -rf /workspace/.build.lock

rsync -a --delete --exclude=.git --exclude=build --exclude=.gradle --exclude=.kotlin \
    --exclude=local.properties --exclude=.build.lock --exclude=artifacts \
    --exclude=.mitv-ci-cache \
    --exclude=.env --exclude='*.keystore' --exclude='*.jks' \
    --exclude=/app/desktop/appResources --exclude=.readme \
    /source/ /workspace/

printf '%s\n' 'sdk.dir=/opt/android-sdk' 'jvm.toolchain.vendor=adoptium' \
    'ani.enable.ios=false' 'ani.enable.firebase=false' > /workspace/local.properties

if [ "$#" -eq 0 ]; then
    set -- :app:android:assembleDefaultTvDebug
fi

set +e
bash /workspace/scripts/gradle-lock.sh "$@" \
    -Pani.android.mitv=true -Pani.android.abis="${MITV_ABIS:-armeabi-v7a}" \
    -Pani.repository=CharlesHYF/izuko-tv \
    -Dorg.gradle.jvmargs='-Xmx3g -Dfile.encoding=UTF-8' \
    -Pkotlin.daemon.jvmargs=-Xmx1536m --max-workers=2 --console=plain 2>&1 | tee /out/build.log
build_status=${PIPESTATUS[0]}
set -e

if [[ "$*" == *testAndroidHostTest* ]]; then
    for module in app-data video-player; do
        report_dir="/workspace/app/shared/$module/build/test-results/testAndroidHostTest"
        if [ -d "$report_dir" ]; then
            mkdir -p "/out/tests/$module"
            for report in "$report_dir"/*.xml; do
                if [ -f "$report" ]; then
                    cp "$report" "/out/tests/$module/"
                fi
            done
        fi
    done
fi

if [ "$build_status" -eq 0 ] && [[ "$*" == *assemble* ]] && [ -d /workspace/app/android/build/outputs/apk ]; then
    cp -R /workspace/app/android/build/outputs/apk /out/
fi
exit "$build_status"
