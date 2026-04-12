package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode
import java.util.UUID

class FinancialGoalUnsupportedCurrencyException(
    goalId: UUID,
    currency: String,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(400),
        messageI18n = "apiErrors.financialGoalUnsupportedCurrency",
        argsI18n = mapOf("goalId" to goalId, "currency" to currency),
        alternativeMessage = "Currency $currency is not targeted by goal $goalId.",
    )
