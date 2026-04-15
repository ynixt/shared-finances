package com.ynixt.sharedfinances.domain.exceptions.http.auth

import com.ynixt.sharedfinances.domain.exceptions.http.AppResponseException
import org.springframework.http.HttpStatusCode

class PasswordRecoveryDisabledException :
    AppResponseException(
        statusCode = HttpStatusCode.valueOf(404),
        messageI18n = "apiErrors.auth.passwordRecoveryDisabled",
        alternativeMessage = "Password recovery is disabled.",
    )
