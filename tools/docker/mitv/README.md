# 小米电视 Docker 构建

适配目标为 MiTV4-ANSM0：Android 6.0.1 / API 23、armeabi-v7a、1GB 内存。Docker 内完成 JDK、SDK、Gradle 与项目构建，宿主机只需 Docker 和 Bash。

## 创建环境

在仓库根目录执行：

```bash
docker build --platform linux/amd64 -t izuko-mitv-build:local tools/docker/mitv
```

基础镜像按摘要固定，JDK 21；Android SDK 使用 API 37 和 build-tools 36.0.0。构建用的 SDK 与 APK 支持的最低 API 是不同概念。

## 构建与测试

镜像创建后，`make verify` 执行空白检查、Bash 语法检查、源码同步回归、app-data 与 video-player 的全部 Android host tests、运行依赖审计和 debug APK 构建。它覆盖本次兼容改造，整仓其他功能的测试仍由原有 Gradle 检查执行。

```bash
bash tools/docker/mitv/run.sh :app:android:assembleDefaultTvDebug
bash tools/docker/mitv/run.sh :app:android:auditMiTvDependencies
bash tools/docker/mitv/run.sh :app:shared:app-data:testAndroidHostTest --tests '*MiTvSettingsDefaultsTest'
bash tools/docker/mitv/run.sh :app:shared:video-player:testAndroidHostTest --tests '*ThrottledSourceLoadControlTest'
```

脚本固定传入 `ani.android.mitv=true`、`ani.android.abis=armeabi-v7a` 和 fork 仓库地址。正式版本、其他平台和既有 API 25 兼容包不启用此开关。

输出目录默认为 `artifacts/mitv`，可通过 `MITV_OUTPUT_DIR` 指定。目录包含最近一次构建日志及已执行测试的 JUnit XML，只有 assemble 成功后才复制 APK；构建失败时不能把目录里已有的 APK 当作本次产物。

容器固定命名为 `izuko-mitv-build`，同一时间只运行一次，避免共享卷并发写入。容器退出后自动删除，`izuko-mitv-gradle` 和 `izuko-mitv-workspace` 两个任务专属卷保存缓存。迁移设备时重新构建镜像即可，卷不是必需输入。

构建容器默认最多使用 6GiB 内存、3 个 CPU，不使用额外 swap；可用 `MITV_MEMORY` 和 `MITV_CPUS` 调整。这是编译工具的资源限制，不代表目标电视的运行内存。

CI 使用 `MITV_GRADLE_CACHE_DIR` 指定任务专属缓存目录并交给 Actions Cache 保存。该参数可以用于独立的本地任务目录，不应指向宿主机已有的全局 Gradle 缓存。

CI 在 ARM32 构建通过后，单独使用 `MITV_ABIS=x86` 构建模拟器测试包，并在有 KVM 的 Linux runner 内通过 Docker 启动官方 API 23 Android TV 镜像。模拟器配置为 1GB 内存、256MB 堆、720p 和 213dpi，检查安装、启动、控制台读取及前台状态。x86 包只用于模拟器测试，电视安装包仍为 armeabi-v7a。

模拟器日志、内存快照和截图保存在临时 Actions artifact 中；控制台访问令牌在日志导出时屏蔽。这项测试不连接共享真机，不作为电视硬解与流畅度的验收证据。

## 隔离边界

- 宿主机源码目录只读挂载，源代码复制到工作卷后构建，Gradle 经 `scripts/gradle-lock.sh` 执行。
- 宿主机的 JDK、SDK、Gradle 缓存、用户主目录和 Docker socket 不挂载进容器。
- 不复制本机 `local.properties`、`.env`、JKS 或 keystore。需要服务凭据或正式签名时，应增加显式、只读的任务专用配置输入，不从宿主机环境自动搜集。
- 容器内 `local.properties` 仅包含容器 SDK 路径、JDK 厂商及关闭 iOS/Firebase 的设置。
- 不运行全局 prune，也不修改其他项目的容器、卷或镜像。

## 验证边界

依赖审计检查实际运行 AAR 的最低 API 与 ARM32 原生库是否存在，不能替代原生符号、解码器和驱动的真机验证。debug APK 用于功能排查，流畅度验收使用优化后的 release 包。

当前 ARM64 Mac 的 Docker 没有 KVM；此环境提供构建与 host tests，不宣称已经运行 API 23 模拟器或复现目标电视性能。
