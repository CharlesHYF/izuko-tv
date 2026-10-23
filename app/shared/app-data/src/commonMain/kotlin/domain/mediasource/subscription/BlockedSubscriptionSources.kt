/*
 * 订阅导入时屏蔽指定数据源
 * 创建日期：2026-10-23
 * 修改日期：2026-10-23
 */
package me.him188.ani.app.domain.mediasource.subscription

/**
 * 订阅里名称命中屏蔽规则的数据源不导入. 内置的第三方订阅文件不由本仓库维护, 订阅更新会周期性重新下发全部数据源,
 * 所以在导入时过滤; 已装上的同名数据源在下一次订阅更新时因不在新列表里而被移除.
 */
internal object BlockedSubscriptionSources {
    /** hanime 系列 (hanime1_1080p、hanime1_720p 等) 不在本应用提供. */
    private val blockedNamePrefixes = listOf("hanime")

    fun isBlocked(name: String): Boolean = blockedNamePrefixes.any { name.startsWith(it, ignoreCase = true) }
}
