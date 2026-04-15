package com.ynixt.sharedfinances.domain.exceptions.http.auth

import com.ynixt.sharedfinances.domain.exceptions.http.AppResponseException
import org.springframework.http.HttpStatusCode

class EmailConfirmationDisabledException :
    AppResponseException(
        statusCode = HttpStatusCode.valueOf(404),
        messageI18n = "apiErrors.auth.emailConfirmationDisabled",
        alternativeMessage = "Email confirmation flow is disabled.",
    )
