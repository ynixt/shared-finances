package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatus

class InvalidOverviewMonthException(
    value: String?,
) : AppResponseException(
        statusCode = HttpStatus.BAD_REQUEST,
        messageI18n = "apiErrors.invalidOverviewMonth",
        alternativeMessage = "Invalid overview month format: ${value ?: "null"}",
        argsI18n = mapOf("value" to (value ?: "null")),
    )
