package me.him188.ani.app.data.models.preference

import me.him188.ani.datasources.api.topic.Resolution

/**
 * 提供低内存电视首次使用的设置默认值，显式保存的用户选择优先
 * 创建日期：2026-10-22
 * 修改日期：2026-10-22
 */
object MiTvSettingsDefaults {
    val mediaPreference: MediaPreference = MediaPreference.PlatformDefault.copy(
        resolution = Resolution.R1080P.id,
        fallbackResolutions = listOf(Resolution.R1080P.id, Resolution.R720P.id, Resolution.R480P.id),
    )

    val theme: ThemeSettings = ThemeSettings.Default.copy(
        tvVisualEffects = TvVisualEffectsLevel.Smooth,
        tvWallBackdropBlur = TvBackdropBlurLevel.None,
        tvDetailsBackdropBlur = TvBackdropBlurLevel.None,
        tvRetainPlaybackSession = false,
    )

    val video: VideoScaffoldConfig = VideoScaffoldConfig.Default.copy(
        enableFramePreview = false,
        enablePlaybackDiskCache = false,
        enableHighQualityAudioTimeStretch = false,
    )

    val selector: MediaSelectorSettings = MediaSelectorSettings.Default.copy(
        enableImageCaptchaAutoSolve = false,
        alwaysFullSearch = false,
    )
}
