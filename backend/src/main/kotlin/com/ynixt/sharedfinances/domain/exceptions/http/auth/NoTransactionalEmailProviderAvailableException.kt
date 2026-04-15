package com.ynixt.sharedfinances.domain.exceptions.http.auth

import com.ynixt.sharedfinances.domain.exceptions.http.AppResponseException
import org.springframework.http.HttpStatusCode

class NoTransactionalEmailProviderAvailableException :
    AppResponseException(
        statusCode = HttpStatusCode.valueOf(503),
        messageI18n = "apiErrors.auth.emailProviderQuotaExhausted",
        alternativeMessage = "No email provider with available quota.",
    )
