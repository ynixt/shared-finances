package com.ynixt.sharedfinances.resources.services.mail

import com.ynixt.sharedfinances.domain.entities.UserEntity
import org.junit.jupiter.api.Test
import org.springframework.context.support.ResourceBundleMessageSource
import java.time.OffsetDateTime
import kotlin.test.assertContains
import kotlin.test.assertFalse

class AccountLifecycleMailMessageComposerTest {
    private val composer =
        AccountLifecycleMailMessageComposer(
            ResourceBundleMessageSource().apply {
                setBasename("i18n/mail/messages")
                setDefaultEncoding("UTF-8")
                setFallbackToSystemLocale(false)
            },
        )

    @Test
    fun `all warning stages compose in English and Portuguese`() {
        for (stage in listOf(30, 7, 1)) {
            val english = composer.buildInactivityWarning(user("en-US"), stage, DELETION_AT, emptyList())
            val portuguese = composer.buildInactivityWarning(user("pt-BR"), stage, DELETION_AT, emptyList())

            assertContains(english.textBody, "Sign in")
            assertContains(portuguese.textBody, "Entre na conta")
            assertContains(english.textBody, "2027")
            assertContains(portuguese.textBody, "2027")
        }
    }

    @Test
    fun `owned group variant names groups while no-group variant does not mention them`() {
        val withGroups = composer.buildInactivityWarning(user("en-US"), 30, DELETION_AT, listOf("Home", "Trip"))
        val withoutGroups = composer.buildInactivityWarning(user("en-US"), 30, DELETION_AT, emptyList())

        assertContains(withGroups.textBody, "Home, Trip")
        assertContains(withGroups.textBody, "groups")
        assertFalse(withoutGroups.textBody.contains("group", ignoreCase = true))
    }

    private fun user(lang: String) =
        UserEntity(
            email = "person@example.com",
            passwordHash = null,
            firstName = "Person",
            lastName = "Test",
            lang = lang,
            defaultCurrency = "USD",
            tmz = "UTC",
            photoUrl = null,
            emailVerified = true,
            mfaEnabled = false,
            totpSecret = null,
            onboardingDone = true,
        )

    companion object {
        val DELETION_AT: OffsetDateTime = OffsetDateTime.parse("2027-01-15T00:00:00Z")
    }
}
