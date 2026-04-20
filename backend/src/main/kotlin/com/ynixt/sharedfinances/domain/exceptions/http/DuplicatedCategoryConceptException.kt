package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode
import java.util.UUID

class DuplicatedCategoryConceptException(
    userId: UUID?,
    groupId: UUID?,
    conceptId: UUID,
    cause: Throwable? = null,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(400),
        messageI18n = "apiErrors.duplicatedCategoryConcept",
        argsI18n =
            mapOf<String, Any>(
                "userId" to (userId ?: ""),
                "groupId" to (groupId ?: ""),
                "conceptId" to conceptId,
            ),
        alternativeMessage = "This concept is already in use by another category in the same scope.",
        cause = cause,
    )
