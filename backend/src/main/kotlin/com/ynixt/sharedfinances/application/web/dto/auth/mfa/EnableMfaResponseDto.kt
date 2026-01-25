package com.ynixt.sharedfinances.application.web.dto.auth.mfa

import java.util.UUID

data class EnableMfaResponseDto(
    val enrollmentId: UUID,
    val secretBase32: String,
    val otpauthUri: String,
)
