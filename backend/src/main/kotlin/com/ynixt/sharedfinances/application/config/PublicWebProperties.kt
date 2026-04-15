package com.ynixt.sharedfinances.application.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.public-web")
data class PublicWebProperties(
    /** Base URL of the SPA (no trailing slash), used in transactional auth email links. */
    val baseUrl: String = "http://localhost:4200",
)
