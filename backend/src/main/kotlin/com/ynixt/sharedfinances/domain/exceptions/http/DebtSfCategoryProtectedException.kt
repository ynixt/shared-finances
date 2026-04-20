package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode
import java.util.UUID

class DebtSfCategoryProtectedException(
    categoryId: UUID,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(400),
        messageI18n = "apiErrors.debtSfCategoryProtected",
        argsI18n =
            mapOf<String, Any>(
                "categoryId" to categoryId,
            ),
        alternativeMessage = "The DEBT_SF category is protected and cannot be deleted or rebound.",
    )
