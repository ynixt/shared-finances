package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode

class ExportLineLimitExceededException(
    matched: Long,
    cap: Int,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(400),
        messageI18n = "apiErrors.exports.lineLimitExceeded",
        argsI18n = mapOf("matched" to matched, "cap" to cap),
        alternativeMessage = "$matched transactions matched, but this export is limited to $cap lines. Narrow the filters.",
    )
