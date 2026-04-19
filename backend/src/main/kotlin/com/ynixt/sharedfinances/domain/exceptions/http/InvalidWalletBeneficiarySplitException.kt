package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatus

class InvalidWalletBeneficiarySplitException(
    message: String,
) : AppResponseException(
        statusCode = HttpStatus.BAD_REQUEST,
        messageI18n = "apiErrors.invalidWalletBeneficiarySplit",
        alternativeMessage = message,
    )
