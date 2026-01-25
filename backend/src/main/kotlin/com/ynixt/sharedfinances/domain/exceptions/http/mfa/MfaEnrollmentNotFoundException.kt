package com.ynixt.sharedfinances.domain.exceptions.http.mfa

import com.ynixt.sharedfinances.domain.exceptions.http.AppResponseException
import org.springframework.http.HttpStatusCode

class MfaEnrollmentNotFoundException(
    cause: Throwable? = null,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(400),
        messageI18n = "apiErrors.mfaEnrollmentNotFound",
        alternativeMessage = "MFA enrollment not found.",
        cause = cause,
    )
