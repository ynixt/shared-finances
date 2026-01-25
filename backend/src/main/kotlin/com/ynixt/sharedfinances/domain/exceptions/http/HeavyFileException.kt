package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode

class HeavyFileException(
    maxSize: Int,
    cause: Throwable? = null,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(400),
        messageI18n = "apiErrors.heavyFile",
        argsI18n =
            mapOf<String, Any>(
                "maxSize" to maxSize,
            ),
        alternativeMessage = "File cannot be have more than $maxSize bytes.",
        cause = cause,
    )
