package com.ynixt.sharedfinances.application.web.dto.auth

data class LoginResultDto(
    val accessToken: String,
    val refreshToken: String,
    val refreshExpiresInSeconds: Long,
)
