#!/usr/bin/env bash
# 在独立卷中运行小米电视兼容包构建
# 创建日期：2026-10-22
# 修改日期：2026-10-22
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
output_dir="${MITV_OUTPUT_DIR:-$project_root/artifacts/mitv}"
mkdir -p "$output_dir"

docker run --rm --platform linux/amd64 --name izuko-mitv-build \
    --mount "type=bind,source=$project_root,target=/source,readonly" \
    --mount "type=bind,source=$output_dir,target=/out" \
    --mount type=volume,source=izuko-mitv-workspace,target=/workspace \
    --mount type=volume,source=izuko-mitv-gradle,target=/cache/gradle \
    izuko-mitv-build:local bash /source/tools/docker/mitv/run-inside.sh "$@"
