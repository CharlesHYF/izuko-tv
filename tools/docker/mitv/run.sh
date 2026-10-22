#!/usr/bin/env bash
# 在独立卷中运行小米电视兼容包构建
# 创建日期：2026-10-22
# 修改日期：2026-10-22
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
output_dir="${MITV_OUTPUT_DIR:-$project_root/artifacts/mitv}"
memory_limit="${MITV_MEMORY:-6g}"
cpu_limit="${MITV_CPUS:-3}"
gradle_mount='type=volume,source=izuko-mitv-gradle,target=/cache/gradle'
mkdir -p "$output_dir"
if [ -n "${MITV_GRADLE_CACHE_DIR:-}" ]; then
    mkdir -p "$MITV_GRADLE_CACHE_DIR"
    cache_dir="$(cd "$MITV_GRADLE_CACHE_DIR" && pwd)"
    gradle_mount="type=bind,source=$cache_dir,target=/cache/gradle"
fi

docker run --rm --platform linux/amd64 --name izuko-mitv-build \
    --env "MITV_ABIS=${MITV_ABIS:-armeabi-v7a}" \
    --env "MITV_GRADLE_HEAP=${MITV_GRADLE_HEAP:-3g}" \
    --env "MITV_KOTLIN_HEAP=${MITV_KOTLIN_HEAP:-1536m}" \
    --env "MITV_WORKERS=${MITV_WORKERS:-2}" \
    --memory "$memory_limit" --memory-swap "$memory_limit" --cpus "$cpu_limit" \
    --mount "type=bind,source=$project_root,target=/source,readonly" \
    --mount "type=bind,source=$output_dir,target=/out" \
    --mount type=volume,source=izuko-mitv-workspace,target=/workspace \
    --mount "$gradle_mount" \
    izuko-mitv-build:local bash /source/tools/docker/mitv/run-inside.sh "$@"
