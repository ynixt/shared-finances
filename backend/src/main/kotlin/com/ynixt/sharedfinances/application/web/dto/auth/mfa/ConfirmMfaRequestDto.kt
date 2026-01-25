package com.ynixt.sharedfinances.application.web.dto.auth.mfa

import java.util.UUID

data class ConfirmMfaRequestDto(
    val enrollmentId: UUID,
    val code: String,
)
