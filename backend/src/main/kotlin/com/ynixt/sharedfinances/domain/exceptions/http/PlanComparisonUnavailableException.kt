package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode

class PlanComparisonUnavailableException :
    AppResponseException(
        statusCode = HttpStatusCode.valueOf(404),
        messageI18n = "apiErrors.planComparison.unavailable",
        alternativeMessage = "The public plan comparison is unavailable while limits are disabled.",
    )
