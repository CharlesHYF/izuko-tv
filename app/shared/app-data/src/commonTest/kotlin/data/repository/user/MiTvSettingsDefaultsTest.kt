package me.him188.ani.app.data.repository.user

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.him188.ani.app.data.models.preference.TvVisualEffectsLevel
import me.him188.ani.app.data.persistent.MemoryDataStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 验证低内存电视默认设置和用户选择的持久化优先级
 * 创建日期：2026-10-22
 * 修改日期：2026-10-22
 */
class MiTvSettingsDefaultsTest {
    @Test
    fun `fresh low memory settings disable costly optional features`() = runTest {
        val settings = PreferencesRepositoryImpl(MemoryDataStore(mutablePreferencesOf()), lowMemoryDefaults = true)
        assertFalse(settings.danmakuEnabled.flow.first())
        assertFalse(settings.videoScaffoldConfig.flow.first().enableFramePreview)
        assertFalse(settings.themeSettings.flow.first().tvRetainPlaybackSession)
        assertEquals(TvVisualEffectsLevel.Smooth, settings.themeSettings.flow.first().visualEffects)
        assertFalse(settings.mediaSelectorSettings.flow.first().enableImageCaptchaAutoSolve)
    }

    @Test
    fun `enabled optional features survive repository recreation`() = runTest {
        val store = MemoryDataStore<Preferences>(mutablePreferencesOf())
        val settings = PreferencesRepositoryImpl(store, lowMemoryDefaults = true)
        settings.danmakuEnabled.set(true)
        settings.videoScaffoldConfig.update { copy(enableFramePreview = true) }
        settings.themeSettings.update { copy(tvRetainPlaybackSession = true) }
        val restored = PreferencesRepositoryImpl(store, lowMemoryDefaults = true)
        assertTrue(restored.danmakuEnabled.flow.first())
        assertTrue(restored.videoScaffoldConfig.flow.first().enableFramePreview)
        assertTrue(restored.themeSettings.flow.first().tvRetainPlaybackSession)
        assertEquals(TvVisualEffectsLevel.Smooth, restored.themeSettings.flow.first().visualEffects)
    }

    @Test
    fun `standard builds retain their existing defaults`() = runTest {
        val settings = PreferencesRepositoryImpl(MemoryDataStore(mutablePreferencesOf()), lowMemoryDefaults = false)
        assertTrue(settings.danmakuEnabled.flow.first())
        assertTrue(settings.videoScaffoldConfig.flow.first().enableFramePreview)
        assertTrue(settings.themeSettings.flow.first().tvRetainPlaybackSession)
    }
}
