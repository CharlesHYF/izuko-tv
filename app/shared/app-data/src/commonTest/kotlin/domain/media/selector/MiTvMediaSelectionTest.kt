package me.him188.ani.app.domain.media.selector

import me.him188.ani.app.data.models.preference.MediaPreference
import me.him188.ani.app.data.models.preference.MiTvSettingsDefaults
import me.him188.ani.app.domain.media.selector.testFramework.runSimpleMediaSelectorTestSuite
import me.him188.ani.datasources.api.topic.Resolution
import me.him188.ani.test.TestContainer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 验证小米电视分辨率偏好的回退和用户调整
 * 创建日期：2026-10-22
 * 修改日期：2026-10-22
 */
@TestContainer
class MiTvMediaSelectionTest {
    @Test
    fun `720p remains playable when 1080p is unavailable`() = runSimpleMediaSelectorTestSuite {
        preferenceApi.savedUserPreference.value = MediaPreference.Empty
        preferenceApi.savedDefaultPreference.value = MiTvSettingsDefaults.mediaPreference
        val candidate = mediaApi.addMedia(media(resolution = Resolution.R720P.id))
        assertEquals(candidate, selector.trySelectDefault())
    }

    @Test
    fun `default preference selects 1080p before 4k`() = runSimpleMediaSelectorTestSuite {
        preferenceApi.savedUserPreference.value = MediaPreference.Empty
        preferenceApi.savedDefaultPreference.value = MiTvSettingsDefaults.mediaPreference
        mediaApi.addMedia(media(resolution = Resolution.R2160P.id))
        val candidate = mediaApi.addMedia(media(resolution = Resolution.R1080P.id))
        assertEquals(candidate, selector.trySelectDefault())
    }

    @Test
    fun `user may prefer 720p to reduce network demand`() = runSimpleMediaSelectorTestSuite {
        preferenceApi.savedUserPreference.value = MediaPreference.Empty
        preferenceApi.savedDefaultPreference.value = MiTvSettingsDefaults.mediaPreference.copy(
            fallbackResolutions = listOf(Resolution.R720P.id, Resolution.R1080P.id),
        )
        mediaApi.addMedia(media(resolution = Resolution.R1080P.id))
        val candidate = mediaApi.addMedia(media(resolution = Resolution.R720P.id))
        assertEquals(candidate, selector.trySelectDefault())
    }
}
