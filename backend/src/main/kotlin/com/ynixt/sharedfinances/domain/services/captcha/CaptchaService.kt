package com.ynixt.sharedfinances.domain.services.captcha

import java.net.InetAddress

/**
 * Application-level captcha gate (e.g. Cloudflare Turnstile). When the feature is off in config,
 * [verifyCaptchaIfNeeded] is a no-op.
 */
interface CaptchaService {
    suspend fun verifyCaptchaIfNeeded(
        token: String?,
        remoteIp: InetAddress?,
    )
}
