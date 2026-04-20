package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode

class InvalidCategoryConceptSelectionException(
    message: String,
) : AppResponseException(
        statusCode = HttpStatusCode.valueOf(400),
        messageI18n = "apiErrors.invalidCategoryConceptSelection",
        alternativeMessage = message,
    )
