package com.ynixt.sharedfinances.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import java.time.OffsetDateTime
import java.util.Optional.of

@Configuration()
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
class JpaAuditConfig {
    @Bean
    fun auditingDateTimeProvider() = DateTimeProvider { of(OffsetDateTime.now()) }
}
