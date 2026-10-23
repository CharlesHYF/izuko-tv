package me.him188.ani.app.domain.mediasource.subscription

import me.him188.ani.app.domain.mediasource.codec.MediaSourceCodecManager
import me.him188.ani.app.domain.mediasource.web.SelectorMediaSource
import me.him188.ani.app.domain.mediasource.web.SelectorMediaSourceArguments
import me.him188.ani.app.domain.mediasource.web.format.SelectorChannelFormatIndexGrouped
import me.him188.ani.app.domain.mediasource.web.format.SelectorSubjectFormatIndexed
import me.him188.ani.test.readTestResourceAsString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 校验仓库发布的 KazumiTV 订阅及其选择器参数
 * 创建日期：2026-10-23
 * 修改日期：2026-10-23
 */
class KazumiTvSubscriptionTest {
    @Test
    fun `repository subscription decodes two web selector sources`() {
        // 测试资源直接来自仓库 subscriptions 目录，与发布文件共用一份 JSON。
        val subscription = MediaSourceCodecManager.json.decodeFromString(
            SubscriptionUpdateData.serializer(),
            readTestResourceAsString("/kazumi-tv.json"),
        )
        val sources = subscription.exportedMediaSourceDataList.mediaSources
        assertEquals(2, sources.size)
        val codecManager = MediaSourceCodecManager()
        val names = mutableListOf<String>()
        for (source in sources) {
            assertEquals(SelectorMediaSource.FactoryId, source.factoryId)
            assertEquals(2, source.version)
            val arguments = assertIs<SelectorMediaSourceArguments>(codecManager.decode(source))
            names.add(arguments.name)
            assertTrue(arguments.description.contains("KazumiTV"))
            val config = arguments.searchConfig
            assertTrue(config.searchUrl.contains("{keyword}"))
            assertEquals(SelectorSubjectFormatIndexed.id, config.subjectFormatId)
            assertTrue(config.selectorSubjectFormatIndexed.isValid())
            assertEquals(SelectorChannelFormatIndexGrouped.id, config.channelFormatId)
            assertTrue(config.selectorChannelFormatFlattened.isValid())
            val videoRegex = assertNotNull(config.matchVideo.matchVideoUrlRegex)
            val nestedRegex = assertNotNull(config.matchVideo.matchNestedUrlRegex)
            val sampleVideo = when (arguments.name) {
                "7sefun" -> "https://mao6.jxdunrui.top/d/file/4169595bac03554cdec4308b9b45db61/%E7%AC%AC01%E9%9B%86.mp4"
                else -> "http://hhjx.hhplayer.com/playlist/8298df34c8e87d6d2e8ffaf1e626c2dd73e6a8a57da322af.m3u8?expires=1792822381&sign=2ca7312b511ea318bb6781cfdacafcc237c752417fe5d2906896afd7a87abc30"
            }
            assertTrue(videoRegex.containsMatchIn(sampleVideo))
            assertFalse(nestedRegex.containsMatchIn(sampleVideo))
            assertFalse(videoRegex.containsMatchIn(config.rawBaseUrl))
        }
        assertEquals(listOf("7sefun", "DM84"), names)
    }
}
