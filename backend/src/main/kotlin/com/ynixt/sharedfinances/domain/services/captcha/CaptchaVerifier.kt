package com.ynixt.sharedfinances.domain.services.captcha

interface CaptchaVerifier {
    suspend fun verify(
        token: String?,
        remoteIp: String?,
    ): Boolean
}
