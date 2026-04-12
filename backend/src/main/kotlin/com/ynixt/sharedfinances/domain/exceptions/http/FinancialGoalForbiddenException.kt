package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode

class FinancialGoalForbiddenException :
    AppResponseException(
        statusCode = HttpStatusCode.valueOf(403),
        messageI18n = "apiErrors.financialGoalForbidden",
        alternativeMessage = "You are not allowed to manage this goal.",
    )
