package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode

class UnauthorizedException :
    AppResponseException(
        statusCode = HttpStatusCode.valueOf(403),
        messageI18n = "apiErrors.unauthorized",
        alternativeMessage = "Unauthorized.",
    )
