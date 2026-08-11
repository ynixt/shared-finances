package com.ynixt.sharedfinances.application.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("app.inactive-account-deletion")
data class InactiveAccountDeletionProperties(
    val enabled: Boolean = false,
    @field:NotBlank val cron: String = "0 0 2 * * *",
)
