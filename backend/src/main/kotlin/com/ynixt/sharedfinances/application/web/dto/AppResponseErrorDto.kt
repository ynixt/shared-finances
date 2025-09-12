package com.ynixt.sharedfinances.application.web.dto

data class AppResponseErrorDto(
    val messageI18n: String? = null,
    val alternativeMessage: String? = null,
    val argsI18n: Map<String, Any>? = null,
)
