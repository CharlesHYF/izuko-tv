package me.him188.ani.app.domain.mediasource.web

import me.him188.ani.app.domain.mediasource.web.captcha.ImageCaptchaRecognizer
import me.him188.ani.app.domain.mediasource.web.captcha.UnsupportedImageCaptchaRecognizer

/**
 * 将 API 23 设备的图片验证码交给人工处理流程
 * 创建日期：2026-10-22
 * 修改日期：2026-10-22
 */
class AndroidOnnxImageCaptchaRecognizer : ImageCaptchaRecognizer by UnsupportedImageCaptchaRecognizer
