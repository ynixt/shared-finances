package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatus

class InvalidGroupDebtAdjustmentException(
    message: String,
) : AppResponseException(
        statusCode = HttpStatus.BAD_REQUEST,
        messageI18n = "apiErrors.groupDebt.invalidAdjustment",
        alternativeMessage = message,
    )
