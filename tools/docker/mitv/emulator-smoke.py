"""验证专属 API 23 TV 模拟器的启动、控制台和前台状态。
创建日期：2026-10-22
修改日期：2026-10-22
"""

import json
import os
from pathlib import Path
import subprocess
import time
from urllib.parse import urlsplit
from urllib.request import Request, urlopen

PACKAGE = os.environ.get("MITV_EMULATOR_PACKAGE", "io.github.grahamzen.anime.tv.debug2")
ACTIVITY = "me.him188.ani.android.activity.MainActivity"
SERIAL = "emulator-5554"
FORWARD_PORT = 17976
STARTUP_TIMEOUT_SECONDS = 120
POLL_SECONDS = 2
ADB_TIMEOUT_SECONDS = 45
HTTP_TIMEOUT_SECONDS = 15
OUTPUT = Path("/out")
REACHABLE_MARKER = "Remote control reachable at "


def adb(*arguments: str, check: bool = True) -> str:
    """只访问当前容器的模拟器。"""
    result = subprocess.run(
        ["adb", "-s", SERIAL, *arguments],
        text=True,
        capture_output=True,
        check=check,
        timeout=ADB_TIMEOUT_SECONDS,
    )
    return result.stdout.replace("\r", "")


def process_id() -> str:
    """兼容 Android 6.0 的 ps 输出。"""
    for line in adb("shell", "ps").splitlines():
        columns = line.split()
        if len(columns) > 1 and columns[-1] == PACKAGE:
            return columns[1]
    return ""


def wait_for_console() -> str:
    """只解析被测包进程的控制台地址，令牌保留在内存中。"""
    deadline = time.monotonic() + STARTUP_TIMEOUT_SECONDS
    while time.monotonic() < deadline:
        pid = process_id()
        if pid:
            for line in adb("logcat", "-d", "-v", "threadtime").splitlines():
                columns = line.split()
                if len(columns) > 2 and columns[2] == pid and REACHABLE_MARKER in line:
                    return line.partition(REACHABLE_MARKER)[2].split()[0]
        time.sleep(POLL_SECONDS)
    raise AssertionError("API 23 app did not expose its console within the startup budget")


def request_json(base: str, route: str, post: bool = False) -> dict:
    """通过应用控制台读取状态或关闭启动提示。"""
    request = Request(base + route, data=b"" if post else None)
    with urlopen(request, timeout=HTTP_TIMEOUT_SECONDS) as response:
        result = json.load(response)
    assert isinstance(result, dict), "Console response must be a JSON object"
    return result


def collect_logs() -> None:
    """保存诊断日志时移除控制台访问令牌。"""
    lines = []
    for line in adb("logcat", "-d", "-v", "threadtime", check=False).splitlines():
        if REACHABLE_MARKER in line:
            line = line.partition(REACHABLE_MARKER)[0] + REACHABLE_MARKER + "<masked>"
        lines.append(line)
    (OUTPUT / "logcat.txt").write_text("\n".join(lines), encoding="utf-8")
    (OUTPUT / "meminfo.txt").write_text(adb("shell", "dumpsys", "meminfo", PACKAGE), encoding="utf-8")


def main() -> None:
    """验证 Android 6.0 上的真实应用进程与控制台。"""
    assert adb("shell", "getprop", "ro.build.version.sdk").strip() == "23"
    assert adb("shell", "getprop", "ro.product.cpu.abi").strip() == "x86"
    adb("logcat", "-c")
    try:
        launch = adb("shell", "am", "start", "-W", "-n", f"{PACKAGE}/{ACTIVITY}")
        (OUTPUT / "launch.txt").write_text(launch, encoding="utf-8")
        console = urlsplit(wait_for_console())
        assert console.port is not None
        adb("forward", f"tcp:{FORWARD_PORT}", f"tcp:{console.port}")
        base = f"http://127.0.0.1:{FORWARD_PORT}{console.path}"
        if not base.endswith("/"):
            base += "/"
        player = request_json(base, "api/player")
        assert isinstance(player.get("available"), bool)
        request_json(base, "api/settings")
        request_json(base, "api/launch-dialog/close", post=True)
        time.sleep(POLL_SECONDS)
        assert process_id(), "App process exited after console requests"
        activities = adb("shell", "dumpsys", "activity", "activities")
        foreground = False
        for line in activities.splitlines():
            if "mResumedActivity" in line and PACKAGE in line:
                foreground = True
        assert foreground, "TV app is not the resumed activity"
        crashes = adb("logcat", "-d", "-b", "crash", check=False)
        assert f"Process: {PACKAGE}," not in crashes, "App crash found in crash buffer"
        adb("shell", "screencap", "-p", "/sdcard/mitv-smoke.png")
        adb("pull", "/sdcard/mitv-smoke.png", str(OUTPUT / "screen.png"))
        summary = {"api": 23, "abi": "x86", "console": True, "foreground": foreground}
        (OUTPUT / "summary.json").write_text(json.dumps(summary), encoding="utf-8")
        print("PASS: API 23 TV startup, console requests and foreground activity")
    finally:
        collect_logs()


if __name__ == "__main__":
    main()
