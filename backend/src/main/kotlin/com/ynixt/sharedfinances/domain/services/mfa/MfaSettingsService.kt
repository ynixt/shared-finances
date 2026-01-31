package com.ynixt.sharedfinances.domain.services.mfa

import com.ynixt.sharedfinances.application.web.dto.auth.mfa.ConfirmMfaResponseDto
import com.ynixt.sharedfinances.application.web.dto.auth.mfa.EnableMfaResponseDto
import java.util.UUID

interface MfaSettingsService {
    suspend fun enableMfaBegin(
        userId: UUID,
        rawPassword: String,
    ): EnableMfaResponseDto

    suspend fun enableMfaConfirm(
        userId: UUID,
        enrollmentId: UUID,
        code: String,
    ): ConfirmMfaResponseDto

    suspend fun disableMfa(
        userId: UUID,
        rawPassword: String,
        code: String,
    )

    suspend fun generateNewRecoveryCodes(userId: UUID): List<String>
}
