/*
 * 验证订阅更新屏蔽 hanime 数据源并移除已装实例
 * 创建日期：2026-10-23
 * 修改日期：2026-10-23
 */
package me.him188.ani.app.domain.mediasource.subscription

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.him188.ani.app.data.persistent.MemoryDataStore
import me.him188.ani.app.data.repository.media.MediaSourceSubscriptionRepository
import me.him188.ani.app.data.repository.media.MediaSourceSubscriptionsSaveData
import me.him188.ani.app.domain.media.fetch.MediaFetcher
import me.him188.ani.app.domain.media.fetch.MediaSourceManager
import me.him188.ani.app.domain.media.selector.MediaSelectorSourceTiers
import me.him188.ani.app.domain.mediasource.codec.ExportedMediaSourceData
import me.him188.ani.app.domain.mediasource.codec.ExportedMediaSourceDataList
import me.him188.ani.app.domain.mediasource.codec.MediaSourceCodecManager
import me.him188.ani.app.domain.mediasource.instance.MediaSourceInstance
import me.him188.ani.app.domain.mediasource.instance.MediaSourceSave
import me.him188.ani.app.domain.mediasource.rule.RuleConfig
import me.him188.ani.app.domain.mediasource.rule.RuleMediaSource
import me.him188.ani.app.domain.mediasource.rule.RuleMediaSourceArguments
import me.him188.ani.app.domain.mediasource.rule.RuleMediaSourceCodec
import me.him188.ani.datasources.api.matcher.MediaSourceWebVideoMatcherLoader
import me.him188.ani.datasources.api.source.FactoryId
import me.him188.ani.datasources.api.source.MediaSourceConfig
import me.him188.ani.datasources.api.source.MediaSourceFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlockedSubscriptionSourcesTest {
    private val subscriptionId = "sub"
    private val remote = listOf(
        rule("稀饭动漫", "a.example.com"),
        rule("hanime1_1080p", "hanime1.example.com"),
        rule("Hanime1_720p", "hanime1.example.com"),
    )
    private val saves = mutableListOf<MediaSourceSave>()

    private fun rule(name: String, site: String) = ExportedMediaSourceData(
        RuleMediaSource.FactoryId,
        RuleMediaSourceCodec.currentVersion,
        MediaSourceCodecManager.json.encodeToJsonElement(
            RuleMediaSourceArguments.serializer(),
            RuleMediaSourceArguments(name = name, iconUrl = "", rule = RuleConfig(baseUrl = "https://$site")),
        ),
    )

    private val manager = object : MediaSourceManager {
        override val allInstances: Flow<List<MediaSourceInstance>> get() = flowOf(emptyList())
        override val allFactories: List<MediaSourceFactory> get() = emptyList()
        override val allFactoryIds: List<FactoryId> get() = emptyList()
        override val mediaFetcher: Flow<MediaFetcher> get() = throw UnsupportedOperationException()
        override val webVideoMatcherLoader: MediaSourceWebVideoMatcherLoader get() = throw UnsupportedOperationException()
        override fun instanceConfigFlow(instanceId: String): Flow<MediaSourceConfig?> =
            flowOf(saves.find { it.instanceId == instanceId }?.config)

        override suspend fun addInstance(instanceId: String, mediaSourceId: String, factoryId: FactoryId, config: MediaSourceConfig) {
            saves += MediaSourceSave(instanceId, mediaSourceId, factoryId, isEnabled = true, config = config)
        }

        override suspend fun getListBySubscriptionId(subscriptionId: String): List<MediaSourceSave> =
            saves.filter { it.config.subscriptionId == subscriptionId }

        override suspend fun partiallyReorderInstances(instanceIds: List<String>) {}

        override suspend fun updateConfig(instanceId: String, config: MediaSourceConfig): Boolean {
            val i = saves.indexOfFirst { it.instanceId == instanceId }
            if (i < 0) return false
            val old = saves[i]
            saves[i] = MediaSourceSave(old.instanceId, old.mediaSourceId, old.factoryId, old.isEnabled, config)
            return true
        }

        override suspend fun setEnabled(instanceId: String, enabled: Boolean) {}

        override suspend fun removeInstance(instanceId: String) {
            saves.removeAll { it.instanceId == instanceId }
        }

        override fun mediaSourceTiersFlow(): Flow<MediaSelectorSourceTiers> = flowOf(MediaSelectorSourceTiers.Empty)
    }

    private val updater = MediaSourceSubscriptionUpdater(
        subscriptions = MediaSourceSubscriptionRepository(
            MemoryDataStore(
                MediaSourceSubscriptionsSaveData(
                    listOf(MediaSourceSubscription(subscriptionId, url = "https://localhost/sub.json")),
                    version = 1,
                ),
            ),
        ),
        mediaSourceManager = manager,
        codecManager = MediaSourceCodecManager(),
        requester = { SubscriptionUpdateData(ExportedMediaSourceDataList(remote)) },
    )

    private fun names() = saves.map {
        MediaSourceCodecManager.json.decodeFromJsonElement(RuleMediaSourceArguments.serializer(), it.config.serializedArguments!!).name
    }

    @Test
    fun `hanime 前缀忽略大小写屏蔽`() {
        assertTrue(BlockedSubscriptionSources.isBlocked("hanime1_1080p"))
        assertTrue(BlockedSubscriptionSources.isBlocked("Hanime1_720p"))
        assertFalse(BlockedSubscriptionSources.isBlocked("稀饭动漫"))
    }

    @Test
    fun `订阅更新不导入 hanime 并移除已装实例`() = runTest {
        // 屏蔽规则生效前已经装上的 hanime 实例
        val installed = rule("hanime1_1080p", "hanime1.example.com")
        manager.addInstance("old", "old", installed.factoryId, MediaSourceConfig(serializedArguments = installed.arguments, subscriptionId = subscriptionId))

        updater.updateAllOutdated(force = true)

        assertEquals(listOf("稀饭动漫"), names())
    }
}
