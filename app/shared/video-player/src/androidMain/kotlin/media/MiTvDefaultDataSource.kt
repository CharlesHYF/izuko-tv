/*
 * 为小米电视默认媒体数据源配置兼容 TLS 客户端
 * 创建日期：2026-10-23
 * 修改日期：2026-10-23
 */
package me.him188.ani.app.videoplayer.media

import android.content.Context
import androidx.annotation.OptIn as AndroidxOptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import me.him188.ani.utils.ktor.miTvTlsEnabled
import me.him188.ani.utils.ktor.withMiTvTls
import okhttp3.OkHttpClient

private val miTvDefaultHttpClient by lazy { OkHttpClient.Builder().withMiTvTls().build() }

/** 文件与内容 URI 使用 Media3 默认处理，HTTPS 回退使用补充信任库。 */
@AndroidxOptIn(UnstableApi::class)
internal fun createMiTvDefaultDataSourceFactory(context: Context): DefaultDataSource.Factory =
    if (miTvTlsEnabled) DefaultDataSource.Factory(context, OkHttpDataSource.Factory(miTvDefaultHttpClient))
    else DefaultDataSource.Factory(context)
