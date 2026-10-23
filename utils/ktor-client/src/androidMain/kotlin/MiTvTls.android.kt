/*
 * 为低于 API 25 的小米电视兼容客户端补充根证书
 * 创建日期：2026-10-23
 * 修改日期：2026-10-23
 */
package me.him188.ani.utils.ktor

import android.os.Build
import okhttp3.OkHttpClient
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

// 只有 MiTV 兼容包的 minSdk 低于 25，其他构建运行时恒为 false。
private const val SYSTEM_ISRG_ROOT_MIN_SDK = 25

fun needsMiTvRoots(sdkInt: Int): Boolean = sdkInt < SYSTEM_ISRG_ROOT_MIN_SDK

val miTvTlsEnabled: Boolean
    get() = needsMiTvRoots(Build.VERSION.SDK_INT)

private val bundledTrustManager by lazy { createX509TrustManager(MiTvRootCertificates.certificates()) }
private val clientTrustManager by lazy { miTvTrustManager(createX509TrustManager(null)) }
private val clientSocketFactory by lazy {
    SSLContext.getInstance("TLS").apply { init(null, arrayOf(clientTrustManager), null) }.socketFactory
}

/** 自建套接字工厂的客户端与其证书链清理器使用同一个组合管理器。 */
fun miTvTrustManager(system: X509TrustManager): X509TrustManager =
    if (miTvTlsEnabled) MiTvTrustManager(system, bundledTrustManager) else system

actual fun OkHttpClient.Builder.withMiTvTls(): OkHttpClient.Builder = apply {
    if (miTvTlsEnabled) sslSocketFactory(clientSocketFactory, clientTrustManager)
}
