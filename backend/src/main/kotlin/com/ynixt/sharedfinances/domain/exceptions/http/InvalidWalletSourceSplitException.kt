package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatus

class InvalidWalletSourceSplitException(
    message: String,
) : AppResponseException(
        statusCode = HttpStatus.BAD_REQUEST,
        messageI18n = "apiErrors.invalidWalletSourceSplit",
        alternativeMessage = message,
    )
