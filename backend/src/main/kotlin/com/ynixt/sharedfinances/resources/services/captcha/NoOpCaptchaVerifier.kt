package com.ynixt.sharedfinances.resources.services.captcha

import com.ynixt.sharedfinances.domain.services.captcha.CaptchaVerifier
import org.springframework.stereotype.Component

@Component
class NoOpCaptchaVerifier : CaptchaVerifier {
    override suspend fun verify(
        token: String?,
        remoteIp: String?,
    ): Boolean = true
}
