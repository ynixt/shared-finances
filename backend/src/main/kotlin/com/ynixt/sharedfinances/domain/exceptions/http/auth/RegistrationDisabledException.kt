package com.ynixt.sharedfinances.domain.exceptions.http.auth

import com.ynixt.sharedfinances.domain.exceptions.http.AppResponseException
import org.springframework.http.HttpStatusCode

class RegistrationDisabledException :
    AppResponseException(
        statusCode = HttpStatusCode.valueOf(403),
        messageI18n = "apiErrors.registration.disabled",
        alternativeMessage = "New user registration is disabled.",
    )
