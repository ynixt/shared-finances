package com.ynixt.sharedfinances.application.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(
    AuthProperties::class,
    PublicWebProperties::class,
)
class AuthConfiguration
