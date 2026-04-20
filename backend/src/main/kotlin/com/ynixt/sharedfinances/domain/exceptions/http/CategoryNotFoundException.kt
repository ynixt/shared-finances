package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode
import java.util.UUID

class CategoryNotFoundException(
    categoryId: UUID,
    cause: Throwable? = null,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(400),
        messageI18n = "apiErrors.categoryNotFound",
        argsI18n =
            mapOf<String, Any>(
                "categoryId" to categoryId,
            ),
        alternativeMessage = "Category $categoryId not found.",
        cause = cause,
    )
