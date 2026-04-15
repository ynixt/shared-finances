package com.ynixt.sharedfinances.application.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.auth")
data class AuthProperties(
    val features: AuthFeatureFlags = AuthFeatureFlags(),
    val emailConfirmation: AuthTokenTtl = AuthTokenTtl(ttlMinutes = 180L),
    val passwordRecovery: AuthTokenTtl = AuthTokenTtl(ttlMinutes = 60L),
    val emailResend: AuthEmailResend = AuthEmailResend(),
    val turnstile: AuthTurnstileProperties = AuthTurnstileProperties(),
    val transactionalMail: AuthTransactionalMailProperties = AuthTransactionalMailProperties(),
)

data class AuthFeatureFlags(
    val emailConfirmationEnabled: Boolean = true,
    val passwordRecoveryEnabled: Boolean = true,
    val turnstileEnabled: Boolean = true,
)

data class AuthTokenTtl(
    val ttlMinutes: Long,
)

data class AuthEmailResend(
    val cooldownSeconds: Long = 60,
)

data class AuthTurnstileProperties(
    val secretKey: String = "",
    val verifyUrl: String = "https://challenges.cloudflare.com/turnstile/v0/siteverify",
)

data class AuthTransactionalMailProperties(
    /**
     * Provider ids tried in order (e.g. `brevo`, `smtp`). Only listed providers are used;
     * there are no separate per-provider `enabled` flags.
     */
    val providerPriority: List<String> = emptyList(),
    val smtp: AuthSmtpMailProperties = AuthSmtpMailProperties(),
    val brevo: AuthBrevoMailProperties = AuthBrevoMailProperties(),
)

data class AuthSmtpMailProperties(
    /**
     * Max sends per UTC day via Redis; `null` or `<= 0` means no cap (default for SMTP).
     * Optional env: `SF_APP_TX_MAIL_SMTP_DAILY_QUOTA`.
     */
    val dailyQuota: Int? = null,
    val fromAddress: String = "noreply@localhost",
    val fromName: String = "Shared Finances",
)

data class AuthBrevoMailProperties(
    /**
     * Max sends per UTC day; `null` or `<= 0` means no cap. Default 300 when unset.
     * Env: `SF_APP_BREVO_DAILY_QUOTA` (use `0` for unlimited).
     */
    val dailyQuota: Int? = 300,
    val apiKey: String = "",
    val fromAddress: String = "",
    val fromName: String = "Shared Finances",
)
