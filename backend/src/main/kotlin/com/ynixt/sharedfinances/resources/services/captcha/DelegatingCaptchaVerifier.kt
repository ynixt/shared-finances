package com.ynixt.sharedfinances.resources.services.captcha

import com.ynixt.sharedfinances.application.config.AuthProperties
import com.ynixt.sharedfinances.domain.services.captcha.CaptchaVerifier
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component
@Primary
class DelegatingCaptchaVerifier(
    private val authProperties: AuthProperties,
    private val cloudflareTurnstileCaptchaVerifier: CloudflareTurnstileCaptchaVerifier,
    private val noOpCaptchaVerifier: NoOpCaptchaVerifier,
) : CaptchaVerifier {
    override suspend fun verify(
        token: String?,
        remoteIp: String?,
    ): Boolean =
        if (authProperties.features.turnstileEnabled) {
            cloudflareTurnstileCaptchaVerifier.verify(token, remoteIp)
        } else {
            noOpCaptchaVerifier.verify(token, remoteIp)
        }
}
