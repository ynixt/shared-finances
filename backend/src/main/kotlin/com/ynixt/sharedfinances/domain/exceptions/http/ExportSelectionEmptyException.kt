package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode

class ExportSelectionEmptyException :
    AppResponseException(
        statusCode = HttpStatusCode.valueOf(400),
        messageI18n = "apiErrors.exports.emptySelection",
        alternativeMessage = "No transaction matched the export filters.",
    )
