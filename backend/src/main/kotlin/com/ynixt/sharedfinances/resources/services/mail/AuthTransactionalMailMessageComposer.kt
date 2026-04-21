package com.ynixt.sharedfinances.resources.services.mail

import com.ynixt.sharedfinances.application.config.AuthProperties
import com.ynixt.sharedfinances.application.config.PublicWebProperties
import com.ynixt.sharedfinances.domain.mail.AuthTransactionalEmailMessage
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

@Service
class AuthTransactionalMailMessageComposer(
    @param:Qualifier("mailMessageSource") private val messageSource: MessageSource,
    private val publicWebProperties: PublicWebProperties,
    private val authProperties: AuthProperties,
) {
    fun buildEmailConfirmation(
        toAddress: String,
        locale: Locale,
        rawToken: String,
    ): AuthTransactionalEmailMessage {
        val link = buildConfirmLink(rawToken)
        val ttlMin = authProperties.emailConfirmation.ttlMinutes
        val window = formatValidityWindow(locale, ttlMin)
        val subject = messageSource.getMessage("mail.auth.emailConfirmation.subject", null, locale)

        val text =
            messageSource.getMessage(
                "mail.auth.emailConfirmation.text",
                arrayOf(window, link, rawToken.chunked(4).joinToString("-")),
                locale,
            )

        return AuthTransactionalEmailMessage(
            toAddress = toAddress,
            subject = subject,
            textBody = text,
        )
    }

    fun buildPasswordReset(
        toAddress: String,
        locale: Locale,
        rawToken: String,
    ): AuthTransactionalEmailMessage {
        val link = buildResetLink(rawToken)
        val ttlMin = authProperties.passwordRecovery.ttlMinutes
        val window = formatValidityWindow(locale, ttlMin)
        val subject = messageSource.getMessage("mail.auth.passwordReset.subject", null, locale)

        val text =
            messageSource.getMessage(
                "mail.auth.passwordReset.text",
                arrayOf(window, link),
                locale,
            )

        return AuthTransactionalEmailMessage(
            toAddress = toAddress,
            subject = subject,
            textBody = text,
        )
    }

    private fun formatValidityWindow(
        locale: Locale,
        ttlMinutes: Long,
    ): String =
        if (ttlMinutes % 60L == 0L) {
            messageSource.getMessage("mail.auth.window.hours", arrayOf(ttlMinutes / 60L), locale)
        } else {
            messageSource.getMessage("mail.auth.window.minutes", arrayOf(ttlMinutes), locale)
        }

    private fun buildConfirmLink(rawToken: String): String {
        val enc = URLEncoder.encode(rawToken, StandardCharsets.UTF_8)
        return "${publicWebProperties.baseUrl.trimEnd('/')}/confirm-email?token=$enc"
    }

    private fun buildResetLink(rawToken: String): String {
        val enc = URLEncoder.encode(rawToken, StandardCharsets.UTF_8)
        return "${publicWebProperties.baseUrl.trimEnd('/')}/reset-password?token=$enc"
    }
}
