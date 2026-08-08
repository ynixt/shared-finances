package com.ynixt.sharedfinances.application.config

import jakarta.validation.constraints.Min
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("app.imports")
data class ImportProperties(
    @field:Min(1)
    val maxLines: Int = 1000,
)
