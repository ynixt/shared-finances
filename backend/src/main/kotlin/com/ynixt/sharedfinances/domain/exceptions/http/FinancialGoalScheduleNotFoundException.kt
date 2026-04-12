package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode
import java.util.UUID

class FinancialGoalScheduleNotFoundException(
    scheduleId: UUID,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(404),
        messageI18n = "apiErrors.financialGoalScheduleNotFound",
        argsI18n = mapOf("scheduleId" to scheduleId),
        alternativeMessage = "Goal schedule $scheduleId not found.",
    )
