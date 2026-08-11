package com.ynixt.sharedfinances.domain.exceptions.http.auth

import com.ynixt.sharedfinances.domain.exceptions.http.AppResponseException
import org.springframework.http.HttpStatusCode

class RegistrationLegalAcceptanceRequiredException :
    AppResponseException(
        statusCode = HttpStatusCode.valueOf(400),
        messageI18n = "apiErrors.registration.termsNotAccepted",
        alternativeMessage = "The terms of use and privacy policy must be accepted.",
    )
