package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode

class InvalidFileTypeException(
    allowedFileTypes: List<String>,
    cause: Throwable? = null,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(400),
        messageI18n = "apiErrors.invalidFileType",
        argsI18n =
            mapOf<String, Any>(
                "allowedFileTypes" to allowedFileTypes,
            ),
        alternativeMessage = "File must be one of the following types: ${allowedFileTypes.joinToString(", ")}.",
        cause = cause,
    )
