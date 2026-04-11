package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatus

class InvalidExchangeRateQuoteCursorException(
    detail: String,
) : AppResponseException(
        statusCode = HttpStatus.BAD_REQUEST,
        messageI18n = "apiErrors.invalidExchangeRateQuoteCursor",
        alternativeMessage = detail,
    )
