package com.ynixt.sharedfinances.application.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import java.nio.charset.StandardCharsets
import java.util.Locale

@Configuration
class MailMessageSourceConfiguration {
    @Bean
    @Qualifier("mailMessageSource")
    fun mailMessageSource(): MessageSource {
        val source = ReloadableResourceBundleMessageSource()

        source.setBasenames(
            "classpath:i18n/mail/messages",
            "classpath:i18n/transaction/messages",
        )

        source.setDefaultEncoding(StandardCharsets.UTF_8.name())
        source.setFallbackToSystemLocale(false)
        source.setDefaultLocale(Locale.ENGLISH)

        return source
    }
}
