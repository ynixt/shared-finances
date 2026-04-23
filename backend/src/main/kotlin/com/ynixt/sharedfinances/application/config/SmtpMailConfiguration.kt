package com.ynixt.sharedfinances.application.config

import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.type.AnnotatedTypeMetadata

@Configuration(proxyBeanMethods = false)
@Conditional(SmtpProviderEnabledCondition::class)
@Import(MailSenderAutoConfiguration::class)
class SmtpMailConfiguration

class SmtpProviderEnabledCondition : Condition {
    override fun matches(
        context: ConditionContext,
        metadata: AnnotatedTypeMetadata,
    ): Boolean {
        val providerPriority: List<String> =
            Binder
                .get(context.environment)
                .bind(
                    "app.auth.transactional-mail.provider-priority",
                    Bindable.listOf(String::class.java),
                ).orElse(emptyList()) ?: emptyList()
        return providerPriority.any { it.trim().equals("smtp", ignoreCase = true) }
    }
}
