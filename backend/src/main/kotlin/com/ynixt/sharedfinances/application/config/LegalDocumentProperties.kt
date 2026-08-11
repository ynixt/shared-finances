package com.ynixt.sharedfinances.application.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.legal")
data class LegalDocumentProperties(
    val enabled: Boolean = false,
    val termsVersion: String = "2026-08-10",
    val privacyVersion: String = "2026-08-10",
)
