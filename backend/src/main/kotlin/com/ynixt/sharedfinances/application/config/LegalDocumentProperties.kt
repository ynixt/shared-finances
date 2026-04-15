package com.ynixt.sharedfinances.application.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.legal")
data class LegalDocumentProperties(
    val termsVersion: String = "2026-04-14",
    val privacyVersion: String = "2026-04-14",
)
