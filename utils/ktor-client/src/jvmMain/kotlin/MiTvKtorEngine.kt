/*
 * 在共享 Ktor 引擎创建时接入平台证书策略
 * 创建日期：2026-10-23
 * 修改日期：2026-10-23
 */
package me.him188.ani.utils.ktor

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig

internal object MiTvKtorEngine : HttpClientEngineFactory<OkHttpConfig> {
    override fun create(block: OkHttpConfig.() -> Unit): HttpClientEngine = OkHttp.create {
        config { withMiTvTls() }
        block()
    }
}
