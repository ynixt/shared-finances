package com.ynixt.sharedfinances.domain.exceptions

import org.springframework.http.HttpStatusCode

class EmailAlreadyInUseException(
    email: String,
    cause: Throwable? = null,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(400),
        messageI18n = "apiErrors.registration.emailAlreadyInUse",
        argsI18n =
            mapOf<String, Any>(
                "email" to email,
            ),
        alternativeMessage = "Email already in use.",
        cause = cause,
    )
