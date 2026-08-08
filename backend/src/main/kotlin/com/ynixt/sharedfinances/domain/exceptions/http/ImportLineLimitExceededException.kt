package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode

class ImportLineLimitExceededException(
    maxLines: Int,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(400),
        messageI18n = "apiErrors.imports.lineLimitExceeded",
        argsI18n = mapOf("maxLines" to maxLines),
        alternativeMessage = "An import cannot contain more than $maxLines lines.",
    )
