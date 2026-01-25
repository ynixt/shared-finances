package com.ynixt.sharedfinances.domain.exceptions.http.mfa

import com.ynixt.sharedfinances.domain.exceptions.http.AppResponseException
import org.springframework.http.HttpStatusCode

class WrongMfaCodeException(
    cause: Throwable? = null,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(401),
        messageI18n = "apiErrors.wrongMfaCode",
        alternativeMessage = "Wrong MFA code.",
        cause = cause,
    )
