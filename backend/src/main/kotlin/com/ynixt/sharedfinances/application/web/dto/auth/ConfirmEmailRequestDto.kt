package com.ynixt.sharedfinances.application.web.dto.auth

import jakarta.validation.constraints.NotBlank

data class ConfirmEmailRequestDto(
    @field:NotBlank(message = "apiErrors.generic.fieldInvalid")
    val token: String,
    val turnstileToken: String? = null,
)
