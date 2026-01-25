package com.ynixt.sharedfinances.application.web.dto.auth.mfa

data class ConfirmMfaResponseDto(
    val recoveryCodes: List<String>,
)
