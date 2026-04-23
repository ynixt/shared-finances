package com.ynixt.sharedfinances.application.config

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.mail.javamail.JavaMailSender
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmtpMailConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(SmtpMailConfiguration::class.java)
            .withPropertyValues(
                "spring.mail.host=localhost",
                "spring.mail.port=1025",
            )

    @Test
    fun `should create JavaMailSender when smtp is in provider-priority`() {
        contextRunner
            .withPropertyValues("app.auth.transactional-mail.provider-priority=smtp,brevo")
            .run { context ->
                assertEquals(1, context.getBeansOfType(JavaMailSender::class.java).size)
            }
    }

    @Test
    fun `should not create JavaMailSender when smtp is not in provider-priority`() {
        contextRunner
            .withPropertyValues("app.auth.transactional-mail.provider-priority=brevo")
            .run { context ->
                assertTrue(context.getBeansOfType(JavaMailSender::class.java).isEmpty())
            }
    }
}
