package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode
import java.util.UUID

class FinancialGoalNotFoundException(
    goalId: UUID,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(404),
        messageI18n = "apiErrors.financialGoalNotFound",
        argsI18n = mapOf("goalId" to goalId),
        alternativeMessage = "Financial goal $goalId not found.",
    )
