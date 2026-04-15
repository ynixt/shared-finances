package com.ynixt.sharedfinances.domain.exceptions.http.auth

import com.ynixt.sharedfinances.domain.exceptions.http.AppResponseException
import org.springframework.http.HttpStatusCode

class AuthTokenInvalidOrExpiredException(
    messageI18nKey: String,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(400),
        messageI18n = messageI18nKey,
        alternativeMessage = "This link is invalid or has expired.",
    )
