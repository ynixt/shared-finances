package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.auth.mfa.ConfirmMfaRequestDto
import com.ynixt.sharedfinances.application.web.dto.auth.mfa.ConfirmMfaResponseDto
import com.ynixt.sharedfinances.application.web.dto.auth.mfa.DisableMfaRequestDto
import com.ynixt.sharedfinances.application.web.dto.auth.mfa.EnableMfaRequestDto
import com.ynixt.sharedfinances.application.web.dto.auth.mfa.EnableMfaResponseDto
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.mfa.MfaSettingsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/mfa-settings")
@Tag(
    name = "MFA",
    description = "Operations related to MFA configuration",
)
class MfaController(
    private val mfaSettingsService: MfaSettingsService,
) {
    @Operation(
        summary = "Begin of enabling MFA",
    )
    @PostMapping("/begin")
    suspend fun enableMfaBegin(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody body: EnableMfaRequestDto,
    ): ResponseEntity<EnableMfaResponseDto> =
        mfaSettingsService
            .enableMfaBegin(
                userId = principalToken.principal.id,
                rawPassword = body.rawPassword,
            ).let { response ->
                ResponseEntity.ok(response)
            }

    @Operation(
        summary = "Confirmation of enabling MFA",
    )
    @PostMapping("confirm")
    suspend fun enableMfaConfirm(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody body: ConfirmMfaRequestDto,
    ): ResponseEntity<ConfirmMfaResponseDto> =
        mfaSettingsService
            .enableMfaConfirm(
                userId = principalToken.principal.id,
                code = body.code,
                enrollmentId = body.enrollmentId,
            ).let {
                ResponseEntity.ok(it)
            }

    @Operation(
        summary = "Disable MFA",
    )
    @PostMapping("disable")
    suspend fun disableMfa(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody body: DisableMfaRequestDto,
    ): ResponseEntity<Void> =
        mfaSettingsService
            .disableMfa(
                userId = principalToken.principal.id,
                rawPassword = body.rawPassword,
                code = body.code,
            ).let {
                ResponseEntity.noContent().build()
            }
}
