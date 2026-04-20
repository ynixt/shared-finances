package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode
import java.util.UUID

class CategoryConceptNotFoundException(
    conceptId: UUID,
    cause: Throwable? = null,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(400),
        messageI18n = "apiErrors.categoryConceptNotFound",
        argsI18n =
            mapOf<String, Any>(
                "conceptId" to conceptId,
            ),
        alternativeMessage = "Category concept $conceptId not found.",
        cause = cause,
    )
