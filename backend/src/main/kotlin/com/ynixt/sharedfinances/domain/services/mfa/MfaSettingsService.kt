package com.ynixt.sharedfinances.domain.services.mfa

import com.ynixt.sharedfinances.application.web.dto.auth.mfa.ConfirmMfaResponseDto
import com.ynixt.sharedfinances.application.web.dto.auth.mfa.EnableMfaResponseDto
import reactor.core.publisher.Mono
import java.util.UUID

interface MfaSettingsService {
    fun enableMfaBegin(
        userId: UUID,
        rawPassword: String,
    ): Mono<EnableMfaResponseDto>

    fun enableMfaConfirm(
        userId: UUID,
        enrollmentId: UUID,
        code: String,
    ): Mono<ConfirmMfaResponseDto>

    fun disableMfa(
        userId: UUID,
        rawPassword: String,
        code: String,
    ): Mono<Unit>

    fun generateNewRecoveryCodes(userId: UUID): Mono<List<String>>
}
