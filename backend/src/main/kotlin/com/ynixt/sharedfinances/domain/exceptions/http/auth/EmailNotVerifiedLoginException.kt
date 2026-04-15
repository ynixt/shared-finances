package com.ynixt.sharedfinances.domain.exceptions.http.auth

import com.ynixt.sharedfinances.domain.exceptions.http.AppResponseException
import org.springframework.http.HttpStatusCode

class EmailNotVerifiedLoginException :
    AppResponseException(
        statusCode = HttpStatusCode.valueOf(401),
        messageI18n = "apiErrors.auth.emailNotVerified",
        alternativeMessage = "Email address is not verified.",
    )
