/*
 * 保留桌面客户端的默认 TLS 配置
 * 创建日期：2026-10-23
 * 修改日期：2026-10-23
 */
package me.him188.ani.utils.ktor

import okhttp3.OkHttpClient

actual fun OkHttpClient.Builder.withMiTvTls(): OkHttpClient.Builder = this
