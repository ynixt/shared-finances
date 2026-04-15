package com.ynixt.sharedfinances.application.web.dto.auth

data class RegisterResultDto(
    val pendingEmailConfirmation: Boolean,
    val email: String,
)
