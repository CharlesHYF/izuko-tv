/*
 * 验证小米电视组合信任管理器与内置根证书
 * 创建日期：2026-10-23
 * 修改日期：2026-10-23
 */
package me.him188.ani.app.domain.foundation

import me.him188.ani.utils.ktor.MiTvRootCertificates
import me.him188.ani.utils.ktor.MiTvTrustManager
import me.him188.ani.utils.ktor.createX509TrustManager
import me.him188.ani.utils.ktor.needsMiTvRoots
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MiTvTrustManagerTest {
    @Test
    fun `系统拒绝的私有根证书链可以由补充信任库验证`() = withCertificates { root, leaf ->
        val system = createX509TrustManager(null)
        val bundled = createX509TrustManager(listOf(root))
        val chain = arrayOf(leaf)
        assertFailsWith<CertificateException> { system.checkServerTrusted(chain, AUTH_TYPE) }
        bundled.checkServerTrusted(chain, AUTH_TYPE)
        MiTvTrustManager(system, bundled).checkServerTrusted(chain, AUTH_TYPE)
    }

    @Test
    fun `两个信任库都拒绝时保留证书异常`() = withCertificates { _, leaf ->
        val bundled = createX509TrustManager(MiTvRootCertificates.certificates())
        val system = createX509TrustManager(null)
        val chain = arrayOf(leaf)
        assertFailsWith<CertificateException> { system.checkServerTrusted(chain, AUTH_TYPE) }
        assertFailsWith<CertificateException> { bundled.checkServerTrusted(chain, AUTH_TYPE) }
        val failure = assertFailsWith<CertificateException> {
            MiTvTrustManager(system, bundled).checkServerTrusted(chain, AUTH_TYPE)
        }
        assertTrue(failure.suppressed.isNotEmpty())
    }

    @Test
    fun `系统通过后不访问补充信任库`() = withCertificates { root, leaf ->
        val unused = object : X509TrustManager {
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String): Unit = error("不应使用补充库")
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String): Unit = error("不应使用补充库")
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        MiTvTrustManager(createX509TrustManager(listOf(root)), unused).checkServerTrusted(arrayOf(leaf), AUTH_TYPE)
    }

    @Test
    fun `接受的签发者合并并去重`() = withCertificates { root, _ ->
        val system = createX509TrustManager(null)
        val bundled = createX509TrustManager(listOf(root))
        val combined = MiTvTrustManager(system, bundled)
        val expected = system.acceptedIssuers.toMutableSet().apply { add(root) }
        assertEquals(expected, combined.acceptedIssuers.toSet())
        assertContentEquals(bundled.acceptedIssuers, MiTvTrustManager(bundled, bundled).acceptedIssuers)
    }

    @Test
    fun `官方根证书可解析且指纹一致`() {
        val roots = MiTvRootCertificates.certificates()
        assertEquals(2, roots.size)
        val expected = listOf(
            "96BCEC06264976F37460779ACF28C5A7CFE8A3C0AAE11A8FFCEE05C0BDDF08C6",
            "69729B8E15A86EFC177A57AFB7171DFC64ADD28C2FCA8CF1507E34453CCB1470",
        )
        for (index in roots.indices) {
            val certificate = roots[index]
            val fingerprint = MessageDigest.getInstance("SHA-256").digest(certificate.encoded)
            val actual = fingerprint.joinToString("") { "%02X".format(it.toInt() and 0xff) }
            assertEquals(expected[index], actual)
            assertTrue(certificate.basicConstraints >= 0)
            certificate.verify(certificate.publicKey)
        }
    }

    @Test
    fun `只有运行系统低于 API 25 时启用补充根`() {
        assertTrue(needsMiTvRoots(sdkInt = 23))
        assertTrue(needsMiTvRoots(sdkInt = 24))
        assertFalse(needsMiTvRoots(sdkInt = 25))
        assertFalse(needsMiTvRoots(sdkInt = 27))
        assertFalse(needsMiTvRoots(sdkInt = 35))
    }

    private fun withCertificates(test: (X509Certificate, X509Certificate) -> Unit) {
        val directory = Files.createTempDirectory("mitv-tls-test").toFile()
        try {
            val store = File(directory, "certificates.p12").absolutePath
            val shared = arrayOf("-keystore", store, "-storetype", "PKCS12", "-storepass", PASSWORD)
            keytool(directory, "-genkeypair", "-alias", "root", "-dname", "CN=MiTV Test Root",
                "-keyalg", "RSA", "-keysize", "2048", "-validity", "2", "-ext", "bc=ca:true",
                "-ext", "ku=keyCertSign,cRLSign", *shared)
            keytool(directory, "-genkeypair", "-alias", "leaf", "-dname", "CN=localhost",
                "-keyalg", "RSA", "-keysize", "2048", "-validity", "2", *shared)
            keytool(directory, "-certreq", "-alias", "leaf", "-file", "leaf.csr", *shared)
            keytool(directory, "-gencert", "-alias", "root", "-infile", "leaf.csr", "-outfile", "leaf.pem",
                "-rfc", "-validity", "2", "-ext", "ku=digitalSignature,keyEncipherment",
                "-ext", "eku=serverAuth", "-ext", "san=dns:localhost", *shared)
            keytool(directory, "-exportcert", "-alias", "root", "-file", "root.pem", "-rfc", *shared)
            test(readCertificate(File(directory, "root.pem")), readCertificate(File(directory, "leaf.pem")))
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun readCertificate(file: File): X509Certificate = file.inputStream().use {
        CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
    }

    private fun keytool(directory: File, vararg arguments: String) {
        val executable = File(System.getProperty("java.home"), "bin/keytool").absolutePath
        val command = mutableListOf(executable)
        command.addAll(arguments)
        val process = ProcessBuilder(command).directory(directory).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        assertEquals(0, process.waitFor(), output)
    }

    private companion object {
        const val PASSWORD = "mitv-test-password"
        const val AUTH_TYPE = "RSA"
    }
}
