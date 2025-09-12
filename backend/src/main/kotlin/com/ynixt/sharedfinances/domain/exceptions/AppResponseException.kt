package com.ynixt.sharedfinances.domain.exceptions

import org.springframework.http.HttpStatusCode

abstract class AppResponseException(
    val statusCode: HttpStatusCode,
    val messageI18n: String,
    val alternativeMessage: String? = null,
    val argsI18n: Map<String, Any>? = null,
    cause: Throwable? = null,
) : RuntimeException(alternativeMessage ?: messageI18n, cause)
