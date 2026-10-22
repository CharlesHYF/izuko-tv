package me.him188.ani.app.videoplayer.media

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import me.him188.ani.app.platform.currentAniBuildConfig
import me.him188.ani.app.videoplayer.player.MiTvPlaybackLimits
import androidx.annotation.OptIn as AndroidxOptIn

/**
 * 限制小米电视兼容包的视频轨道分辨率
 * 创建日期：2026-10-22
 * 修改日期：2026-10-22
 */
@AndroidxOptIn(UnstableApi::class)
internal fun configureMiTvPlayer(builder: ExoPlayer.Builder, context: Context) {
    if (!currentAniBuildConfig.miTvCompatibility) return
    val selector = DefaultTrackSelector(context)
    selector.setParameters(
        selector.buildUponParameters()
            .setMaxVideoSize(MiTvPlaybackLimits.MAX_VIDEO_WIDTH, MiTvPlaybackLimits.MAX_VIDEO_HEIGHT)
            .setExceedVideoConstraintsIfNecessary(false),
    )
    builder.setTrackSelector(selector)
}
