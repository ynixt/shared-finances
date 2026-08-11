package com.ynixt.sharedfinances.resources.services.mail

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.mail.TransactionalEmailMessage
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Service
class AccountLifecycleMailMessageComposer(
    @param:Qualifier("mailMessageSource") private val messageSource: MessageSource,
) {
    fun buildInactivityWarning(
        user: UserEntity,
        stageDays: Int,
        deletionAt: OffsetDateTime,
        ownedGroupNames: List<String>,
    ): TransactionalEmailMessage {
        require(stageDays in WARNING_STAGES) { "Unsupported inactivity warning stage: $stageDays" }
        val locale = UserLocaleResolver.resolve(user.lang)
        val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(deletionAt)
        val key = "mail.account.inactivity.$stageDays"
        val subject = messageSource.getMessage("$key.subject", arrayOf(date), locale)
        val bodyKey = if (ownedGroupNames.isEmpty()) "$key.text" else "$key.textWithGroups"
        val groups = ownedGroupNames.sorted().joinToString(", ")
        val text = messageSource.getMessage(bodyKey, arrayOf(date, groups), locale)
        return TransactionalEmailMessage(user.email, subject, text)
    }

    companion object {
        val WARNING_STAGES = setOf(30, 7, 1)
    }
}
