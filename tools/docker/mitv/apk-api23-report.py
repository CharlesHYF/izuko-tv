"""读取 APK 的 DEX 类型表，检查 Android 6.0 缺失的 Java API 引用。
创建日期：2026-10-22
修改日期：2026-10-22
"""

import argparse
import json
from pathlib import Path
import struct
import zipfile

WATCHED_PREFIXES = (
    "Ljava/util/concurrent/CompletableFuture",
    "Ljava/util/concurrent/CompletionStage;",
    "Ljava/util/stream/",
    "Ljava/util/function/",
    "Ljava/time/",
)
HEADER_STRING_IDS_OFFSET = 56
HEADER_TYPE_IDS_OFFSET = 64
UINT32_BYTES = 4


def read_types(raw: bytes) -> set[str]:
    """只读取 DEX 类型引用，普通日志字符串不参与检查。"""
    if not raw.startswith(b"dex\n"):
        raise ValueError("Unsupported DEX header")
    string_count, string_offset = struct.unpack_from("<II", raw, HEADER_STRING_IDS_OFFSET)
    type_count, type_offset = struct.unpack_from("<II", raw, HEADER_TYPE_IDS_OFFSET)
    found = set()
    for index in range(type_count):
        string_index = struct.unpack_from("<I", raw, type_offset + index * UINT32_BYTES)[0]
        if string_index >= string_count:
            raise ValueError("Invalid DEX type string index")
        offset = struct.unpack_from("<I", raw, string_offset + string_index * UINT32_BYTES)[0]
        while raw[offset] & 0x80:
            offset += 1
        offset += 1
        end = raw.index(b"\x00", offset)
        descriptor = raw[offset:end].decode("utf-8", errors="replace")
        descriptor = descriptor.lstrip("[")
        if descriptor.startswith(WATCHED_PREFIXES):
            found.add(descriptor)
    return found


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    arguments = parser.parse_args()
    references = set()
    dex_count = 0
    with zipfile.ZipFile(arguments.apk) as archive:
        for entry in archive.infolist():
            if entry.filename.startswith("classes") and entry.filename.endswith(".dex") and "/" not in entry.filename:
                references.update(read_types(archive.read(entry)))
                dex_count += 1
    assert dex_count > 0, "APK contains no DEX"
    report = {"dex_count": dex_count, "platform_api_references": sorted(references)}
    arguments.output.parent.mkdir(parents=True, exist_ok=True)
    arguments.output.write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(json.dumps(report, indent=2))
    # 类型存在不能证明路径可达，其他包的受保护调用仍需运行验证。
    for descriptor in references:
        assert not descriptor.startswith("Ljava/util/concurrent/CompletableFuture"), "Unresolved API24 CompletableFuture reference"


if __name__ == "__main__":
    main()
