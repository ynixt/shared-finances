package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatus

class InvalidImportRequestException(
    message: String,
) : AppResponseException(
        statusCode = HttpStatus.BAD_REQUEST,
        messageI18n = "apiErrors.imports.invalidRequest",
        alternativeMessage = message,
    )
