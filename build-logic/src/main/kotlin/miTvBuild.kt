import org.gradle.api.Project

private const val MITV_ADAPTIVE_VERSION = "1.2.0"
private const val MITV_JETBRAINS_NAVIGATION_VERSION = "2.9.2"
private const val MITV_ANDROID_NAVIGATION_VERSION = "2.9.7"

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
            "org.jetbrains.androidx.navigation:navigation-common:$MITV_JETBRAINS_NAVIGATION_VERSION",
            "org.jetbrains.androidx.navigation:navigation-runtime:$MITV_JETBRAINS_NAVIGATION_VERSION",
            "org.jetbrains.androidx.navigation:navigation-compose:$MITV_JETBRAINS_NAVIGATION_VERSION",
            "androidx.navigation:navigation-common:$MITV_ANDROID_NAVIGATION_VERSION",
            "androidx.navigation:navigation-runtime:$MITV_ANDROID_NAVIGATION_VERSION",
            "androidx.navigation:navigation-compose:$MITV_ANDROID_NAVIGATION_VERSION",
            "androidx.navigation:navigation-common-android:$MITV_ANDROID_NAVIGATION_VERSION",
            "androidx.navigation:navigation-runtime-android:$MITV_ANDROID_NAVIGATION_VERSION",
            "androidx.navigation:navigation-compose-android:$MITV_ANDROID_NAVIGATION_VERSION",
        )
        exclude(mapOf("group" to "org.openani.anitorrent", "module" to "anitorrent-native-android"))
        exclude(mapOf("group" to "org.openani.anitorrent", "module" to "anitorrent-native-android-debug"))
    }
}
