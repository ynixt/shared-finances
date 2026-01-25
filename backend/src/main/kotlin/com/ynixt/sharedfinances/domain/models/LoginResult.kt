package com.ynixt.sharedfinances.domain.models

data class LoginResult(
    val accessToken: String,
    val refreshToken: String,
    val refreshExpiresInSeconds: Long,
)
