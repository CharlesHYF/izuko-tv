/*
 * 提供小米电视 TLS 的平台接入点
 * 创建日期：2026-10-23
 * 修改日期：2026-10-23
 */
package me.him188.ani.utils.ktor

import okhttp3.OkHttpClient

expect fun OkHttpClient.Builder.withMiTvTls(): OkHttpClient.Builder
