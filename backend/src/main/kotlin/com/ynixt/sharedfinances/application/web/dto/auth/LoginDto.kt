package com.ynixt.sharedfinances.application.web.dto.auth

data class LoginDto(
    val email: String,
    val password: String,
    val turnstileToken: String? = null,
)
