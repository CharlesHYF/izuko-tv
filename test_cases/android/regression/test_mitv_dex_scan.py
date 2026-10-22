"""验证低版本 API 扫描能区分 DEX 类型、数组及普通字符串。
创建日期：2026-10-22
修改日期：2026-10-22
"""

import importlib.util
from pathlib import Path
import struct
import unittest

SCRIPT = Path(__file__).resolve().parents[3] / "tools/docker/mitv/apk-api23-report.py"
SPEC = importlib.util.spec_from_file_location("mitv_api_scan", SCRIPT)
SCANNER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(SCANNER)
DEX_HEADER_BYTES = 112
UINT32_BYTES = 4
FUTURE = "Ljava/util/concurrent/CompletableFuture;"


def dex_tables(types: list[str], literals: list[str]) -> bytes:
    """构造扫描器消费的 DEX 头和类型字符串表。"""
    strings = types + literals
    string_offset = DEX_HEADER_BYTES
    type_offset = string_offset + len(strings) * UINT32_BYTES
    content_offset = type_offset + len(types) * UINT32_BYTES
    raw = bytearray(content_offset)
    raw[:8] = b"dex\n035\x00"
    struct.pack_into("<II", raw, 56, len(strings), string_offset)
    struct.pack_into("<II", raw, 64, len(types), type_offset)
    for index, value in enumerate(strings):
        encoded = value.encode("utf-8")
        assert len(encoded) < 128
        struct.pack_into("<I", raw, string_offset + index * UINT32_BYTES, len(raw))
        raw.extend(bytes([len(encoded)]) + encoded + b"\x00")
    for index in range(len(types)):
        struct.pack_into("<I", raw, type_offset + index * UINT32_BYTES, index)
    return bytes(raw)


class Api23DexScanTest(unittest.TestCase):
    def test_missing_api_type_is_reported(self) -> None:
        self.assertEqual({FUTURE}, SCANNER.read_types(dex_tables([FUTURE], [])))

    def test_log_literal_does_not_count_as_type_reference(self) -> None:
        raw = dex_tables(["Ljava/lang/String;"], [FUTURE])
        self.assertEqual(set(), SCANNER.read_types(raw))

    def test_array_reference_is_reported(self) -> None:
        self.assertEqual({FUTURE}, SCANNER.read_types(dex_tables(["[[" + FUTURE], [])))

    def test_supported_latch_does_not_trigger(self) -> None:
        raw = dex_tables(["Ljava/util/concurrent/CountDownLatch;"], [])
        self.assertEqual(set(), SCANNER.read_types(raw))

    def test_stream_and_time_types_are_reported(self) -> None:
        types = ["Ljava/util/stream/Stream;", "Ljava/time/Instant;"]
        self.assertEqual(set(types), SCANNER.read_types(dex_tables(types, [])))


if __name__ == "__main__":
    unittest.main()
