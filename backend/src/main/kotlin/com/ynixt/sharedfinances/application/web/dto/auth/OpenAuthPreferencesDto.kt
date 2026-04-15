package com.ynixt.sharedfinances.application.web.dto.auth

/**
 * Public auth-related feature flags for clients (registration, login, recovery, Turnstile).
 */
data class OpenAuthPreferencesDto(
    val emailConfirmationEnabled: Boolean,
    val passwordRecoveryEnabled: Boolean,
    val turnstileEnabled: Boolean,
)
