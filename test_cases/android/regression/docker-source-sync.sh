#!/usr/bin/env bash
# 验证容器源码同步、缓存保留和敏感文件隔离
# 创建日期：2026-10-22
# 修改日期：2026-10-22
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
entry_script="${1:-tools/docker/mitv/run-inside.sh}"

docker run --rm -i --platform linux/amd64 \
    --mount "type=bind,source=$project_root,target=/repo,readonly" \
    izuko-mitv-build:local bash -s -- "$entry_script" <<'BASH'
set -euo pipefail
trap 'printf "FAIL: source synchronization assertion at line %s\n" "$LINENO" >&2' ERR
mkdir -p /source/scripts /source/src /source/.mitv-ci-cache \
    /workspace/src /workspace/build /out
cp "/repo/$1" /source/build-entry.sh
printf '#!/usr/bin/env bash\nexit 0\n' > /source/scripts/gradle-lock.sh
printf 'old source\n' > /workspace/src/removed.kt
printf 'cached output\n' > /workspace/build/cached.txt
printf 'current source\n' > /source/src/current.kt
printf 'private value\n' > /source/.env
printf 'private key\n' > /source/signing.jks
printf 'gradle cache\n' > /source/.mitv-ci-cache/cache.bin

bash /source/build-entry.sh :fixture
test -f /workspace/src/current.kt
test ! -e /workspace/src/removed.kt
test -f /workspace/build/cached.txt
test ! -e /workspace/.env
test ! -e /workspace/signing.jks
test ! -e /workspace/.mitv-ci-cache

mv /source/src/current.kt /source/src/moved.kt
bash /source/build-entry.sh :fixture
test ! -e /workspace/src/current.kt
test -f /workspace/src/moved.kt
test -f /workspace/build/cached.txt
printf 'PASS: source deletion, moves, cached outputs and private files\n'
BASH
