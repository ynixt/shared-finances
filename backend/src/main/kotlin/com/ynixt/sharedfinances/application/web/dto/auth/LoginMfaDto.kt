package com.ynixt.sharedfinances.application.web.dto.auth

import java.util.UUID

data class LoginMfaDto(
    val challengeId: UUID,
    val code: String,
)
