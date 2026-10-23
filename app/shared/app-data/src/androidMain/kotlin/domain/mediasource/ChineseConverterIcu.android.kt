/*
 * 按系统版本提供 ICU 繁简转换
 * 创建日期：2026-10-23
 * 修改日期：2026-10-23
 */
package me.him188.ani.app.domain.mediasource

import android.icu.text.Transliterator
import android.os.Build
import androidx.annotation.RequiresApi

/** android.icu.text.Transliterator 从 API 29 起才公开, 更低的系统上解析它会抛 NoClassDefFoundError. */
internal fun supportsIcuTransliterator(sdkInt: Int): Boolean = sdkInt >= Build.VERSION_CODES.Q

/**
 * 繁转简函数. API 29 以下, 或取不到 ICU 转写器 (host test 的桩 android.jar) 时为 null, 调用方原样返回名字.
 * 返回类型不含 android.icu 类型, 持有它的属性被读取时不会触发 android.icu 的类解析.
 */
internal fun createTraditionalToSimplified(sdkInt: Int = Build.VERSION.SDK_INT): ((String) -> String)? {
    if (!supportsIcuTransliterator(sdkInt)) return null
    return IcuTraditionalToSimplified.create()
}

@RequiresApi(Build.VERSION_CODES.Q)
private object IcuTraditionalToSimplified {
    fun create(): ((String) -> String)? {
        val transliterator = runCatching { Transliterator.getInstance("Traditional-Simplified") }.getOrNull()
            ?: return null
        return transliterator::transliterate
    }
}
