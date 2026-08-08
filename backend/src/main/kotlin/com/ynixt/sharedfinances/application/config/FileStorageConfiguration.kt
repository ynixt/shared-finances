package com.ynixt.sharedfinances.application.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("app.file-storage")
data class FileStorageProperties(
    @field:NotBlank
    val path: String,
)

@Configuration
@EnableConfigurationProperties(FileStorageProperties::class)
class FileStorageConfiguration
