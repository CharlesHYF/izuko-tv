package me.him188.ani.app.videoplayer.player

/**
 * 定义 1GB 小米电视的播放器资源上限
 * 创建日期：2026-10-22
 * 修改日期：2026-10-22
 */
object MiTvPlaybackLimits {
    const val BUFFER_BYTES = 16 * 1024 * 1024
    const val BUFFER_MILLIS = 15_000
    const val MAX_VIDEO_WIDTH = 1920
    const val MAX_VIDEO_HEIGHT = 1080
    const val SUBTITLE_RENDER_PIXELS = 1280 * 720
}
