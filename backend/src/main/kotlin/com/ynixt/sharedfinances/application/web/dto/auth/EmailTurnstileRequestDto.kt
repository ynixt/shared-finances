package com.ynixt.sharedfinances.application.web.dto.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class EmailTurnstileRequestDto(
    @field:NotBlank(message = "apiErrors.registration.emailInvalid")
    @field:Size(min = 1, max = 320, message = "apiErrors.registration.emailSize")
    @field:Email(message = "apiErrors.registration.emailInvalid")
    val email: String,
    val turnstileToken: String? = null,
)
