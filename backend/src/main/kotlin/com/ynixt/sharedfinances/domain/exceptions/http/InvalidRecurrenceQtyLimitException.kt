package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode

class InvalidRecurrenceQtyLimitException(
    requestedQtyLimit: Int,
    alreadyExecuted: Int,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(400),
        messageI18n = "apiErrors.invalidRecurrenceQtyLimit",
        argsI18n =
            mapOf(
                "requestedQtyLimit" to requestedQtyLimit,
                "alreadyExecuted" to alreadyExecuted,
            ),
        alternativeMessage = "Requested recurrence limit ($requestedQtyLimit) cannot be lower than already executed ($alreadyExecuted).",
    )
