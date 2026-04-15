package com.ynixt.sharedfinances.domain.exceptions.http.auth

import com.ynixt.sharedfinances.domain.exceptions.http.AppResponseException
import org.springframework.http.HttpStatusCode

class EmailResendCooldownActiveException(
    retryAfterSeconds: Long,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(429),
        messageI18n = "apiErrors.auth.resendCooldownActive",
        argsI18n = mapOf("retryAfterSeconds" to retryAfterSeconds),
        alternativeMessage = "Please wait before requesting another email.",
    )
