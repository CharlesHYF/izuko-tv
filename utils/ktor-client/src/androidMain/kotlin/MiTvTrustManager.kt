/*
 * 使用系统与内置根证书验证旧版 Android 证书链
 * 创建日期：2026-10-23
 * 修改日期：2026-10-23
 */
package me.him188.ani.utils.ktor

import android.net.http.X509TrustManagerExtensions
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/** 系统校验优先，只有证书校验失败才使用补充信任库。 */
class MiTvTrustManager(
    private val system: X509TrustManager,
    private val bundled: X509TrustManager,
) : X509TrustManager {
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        verify { it.checkServerTrusted(chain, authType) }
    }

    /** Android 和 OkHttp 按签名反射调用，返回验证后的证书链。 */
    fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, host: String): List<X509Certificate> =
        verify { X509TrustManagerExtensions(it).checkServerTrusted(chain, authType, host) }

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        verify { it.checkClientTrusted(chain, authType) }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        val issuers = LinkedHashSet<X509Certificate>()
        for (certificate in system.acceptedIssuers) issuers.add(certificate)
        for (certificate in bundled.acceptedIssuers) issuers.add(certificate)
        return issuers.toTypedArray()
    }

    private inline fun <T> verify(check: (X509TrustManager) -> T): T {
        try {
            return check(system)
        } catch (systemFailure: CertificateException) {
            try {
                return check(bundled)
            } catch (bundledFailure: CertificateException) {
                // 两条校验路径的原因都保留，调用方仍收到证书异常。
                if (bundledFailure !== systemFailure) bundledFailure.addSuppressed(systemFailure)
                throw bundledFailure
            }
        }
    }
}

/** null 使用系统库；非 null 仅信任所给根证书，不读取系统库。 */
fun createX509TrustManager(certificates: List<X509Certificate>?): X509TrustManager {
    val keyStore = if (certificates == null) null else KeyStore.getInstance(KeyStore.getDefaultType()).apply {
        load(null, null)
        for (index in certificates.indices) setCertificateEntry("root-$index", certificates[index])
    }
    val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    factory.init(keyStore)
    for (manager in factory.trustManagers) {
        if (manager is X509TrustManager) return manager
    }
    error("X509TrustManager unavailable")
}
