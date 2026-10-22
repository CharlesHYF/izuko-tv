"""检查优化 APK 的 DEX 预算并生成安装前体积报告。
创建日期：2026-10-22
修改日期：2026-10-22
"""

import argparse
import json
from pathlib import Path
import zipfile

MIB = 1024 * 1024
MAX_DEX_MIB = 40
GENERATOR_DESCRIPTOR = b"Lorg/antlr/v4/Tool;"


def inspect(apk: Path) -> dict:
    """按 ZIP 未压缩长度统计设备安装时需要处理的 DEX。"""
    result = {"apk_bytes": apk.stat().st_size, "dex_bytes": 0, "dex_count": 0, "antlr_generator": False}
    with zipfile.ZipFile(apk) as archive:
        for entry in archive.infolist():
            if entry.filename.startswith("classes") and entry.filename.endswith(".dex") and "/" not in entry.filename:
                result["dex_bytes"] += entry.file_size
                result["dex_count"] += 1
                if GENERATOR_DESCRIPTOR in archive.read(entry):
                    result["antlr_generator"] = True
    assert result["dex_count"] > 0, "APK has no DEX"
    return result


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--baseline", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--allow-over-budget", action="store_true", help="Publish a user-authorized test candidate with the actual budget result")
    arguments = parser.parse_args()
    optimized = inspect(arguments.apk)
    baseline = inspect(arguments.baseline)
    within_budget = optimized["dex_bytes"] <= MAX_DEX_MIB * MIB
    report = {"release": optimized, "debug_baseline": baseline, "max_dex_mib": MAX_DEX_MIB, "within_budget": within_budget}
    arguments.output.mkdir(parents=True, exist_ok=True)
    (arguments.output / "apk-size.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
    text = (
        "| 构建 | APK (MiB) | 未压缩 DEX (MiB) | DEX 数量 |\n"
        "| --- | ---: | ---: | ---: |\n"
        f"| Debug 基线 | {baseline['apk_bytes'] / MIB:.2f} | {baseline['dex_bytes'] / MIB:.2f} | {baseline['dex_count']} |\n"
        f"| Release 优化包 | {optimized['apk_bytes'] / MIB:.2f} | {optimized['dex_bytes'] / MIB:.2f} | {optimized['dex_count']} |\n"
    )
    (arguments.output / "apk-size.md").write_text(text, encoding="utf-8")
    print(text)
    assert within_budget or arguments.allow_over_budget, "Release DEX exceeds 40 MiB; do not install yet"
    assert optimized["dex_bytes"] < baseline["dex_bytes"] / 2, "DEX did not shrink by at least half"
    assert not optimized["antlr_generator"], "ANTLR generator class is present in optimized DEX"


if __name__ == "__main__":
    main()
