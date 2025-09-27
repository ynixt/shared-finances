package com.ynixt.sharedfinances.domain.exceptions

import org.springframework.http.HttpStatusCode
import java.util.UUID

class DuplicatedCategoryException(
    userId: UUID?,
    groupId: UUID?,
    cause: Throwable? = null,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(400),
        messageI18n = "apiErrors.duplicatedCategory",
        argsI18n =
            mapOf<String, Any>(
                "userId" to (userId ?: ""),
                "groupId" to (groupId ?: ""),
            ),
        alternativeMessage = "This name is already in use in another category.",
        cause = cause,
    )
