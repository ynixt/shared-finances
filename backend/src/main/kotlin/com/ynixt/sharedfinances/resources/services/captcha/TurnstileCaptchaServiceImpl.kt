package com.ynixt.sharedfinances.resources.services.captcha

import com.ynixt.sharedfinances.application.config.AuthProperties
import com.ynixt.sharedfinances.domain.exceptions.http.auth.TurnstileVerificationFailedException
import com.ynixt.sharedfinances.domain.services.captcha.CaptchaService
import com.ynixt.sharedfinances.domain.services.captcha.CaptchaVerifier
import org.springframework.stereotype.Service
import java.net.InetAddress

@Service
class TurnstileCaptchaServiceImpl(
    private val authProperties: AuthProperties,
    private val captchaVerifier: CaptchaVerifier,
) : CaptchaService {
    override suspend fun verifyCaptchaIfNeeded(
        token: String?,
        remoteIp: InetAddress?,
    ) {
        if (!authProperties.features.turnstileEnabled) {
            return
        }
        val ipStr = remoteIp?.hostAddress
        if (!captchaVerifier.verify(token, ipStr)) {
            throw TurnstileVerificationFailedException()
        }
    }
}
