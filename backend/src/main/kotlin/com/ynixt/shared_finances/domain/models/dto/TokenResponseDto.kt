package com.ynixt.shared_finances.domain.models.dto

data class TokenResponseDto(
    val access_token: String,
    val refresh_token: String,
    val expires_in: Long,
    val token_type: String,
    val scope: String
)