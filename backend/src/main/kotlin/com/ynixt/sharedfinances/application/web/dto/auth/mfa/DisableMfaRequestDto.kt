package com.ynixt.sharedfinances.application.web.dto.auth.mfa

data class DisableMfaRequestDto(
    val rawPassword: String,
    val code: String,
)
