/*
 * 验证繁简转换在 API 29 以下不使用 android.icu
 * 创建日期：2026-10-23
 * 修改日期：2026-10-23
 */
package me.him188.ani.app.domain.mediasource

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChineseConverterIcuTest {
    @Test
    fun `API 29 以下不创建 ICU 转写器`() {
        assertFalse(supportsIcuTransliterator(23))
        assertFalse(supportsIcuTransliterator(28))
        assertNull(createTraditionalToSimplified(23))
    }

    @Test
    fun `API 29 起支持 ICU 转写器`() {
        assertTrue(supportsIcuTransliterator(29))
    }

    @Test
    fun `取不到转写器时原样返回名字`() {
        assertEquals("進擊的巨人", "進擊的巨人".toSimplifiedChinese())
    }
}
