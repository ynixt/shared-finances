package com.ynixt.sharedfinances.domain.exceptions.http

import org.springframework.http.HttpStatusCode

class InvalidPastScheduledExecutionEditException :
    AppResponseException(
        statusCode = HttpStatusCode.valueOf(400),
        messageI18n = "apiErrors.invalidPastScheduledExecutionEdit",
        alternativeMessage = "The next scheduled execution cannot be moved to the same day as or before the previous generated execution.",
    )
