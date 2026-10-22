import org.gradle.api.Project

private const val MITV_ADAPTIVE_VERSION = "1.2.0"

/**
 * 配置 API 23 小米电视兼容构建的依赖边界
 * 创建日期：2026-10-22
 * 修改日期：2026-10-22
 */
val Project.buildMiTvApp: Boolean
    get() = getPropertyOrNull("ani.android.mitv")?.toBooleanStrict() ?: false

/** 为兼容构建排除要求 API 26 的 Android 本地种子运行库。 */
fun Project.configureMiTvDependencies() {
    if (!buildMiTvApp) return
    configurations.configureEach {
        resolutionStrategy.force(
            "org.jetbrains.compose.material3.adaptive:adaptive:$MITV_ADAPTIVE_VERSION",
            "org.jetbrains.compose.material3.adaptive:adaptive-layout:$MITV_ADAPTIVE_VERSION",
            "org.jetbrains.compose.material3.adaptive:adaptive-navigation:$MITV_ADAPTIVE_VERSION",
        )
        exclude(mapOf("group" to "org.openani.anitorrent", "module" to "anitorrent-native-android"))
        exclude(mapOf("group" to "org.openani.anitorrent", "module" to "anitorrent-native-android-debug"))
    }
}
