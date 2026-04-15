package com.ynixt.sharedfinances.application.web.dto.auth

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PasswordResetConfirmRequestDto(
    @field:NotBlank(message = "apiErrors.generic.fieldInvalid")
    val token: String,
    @field:NotBlank(message = "apiErrors.registration.passwordInvalid")
    @field:Size(min = 6, max = 15, message = "apiErrors.registration.passwordSize")
    val newPassword: String,
    val turnstileToken: String? = null,
)
