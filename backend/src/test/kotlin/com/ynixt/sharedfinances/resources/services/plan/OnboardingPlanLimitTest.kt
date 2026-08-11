package com.ynixt.sharedfinances.resources.services.plan

import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.assertTrue

class OnboardingPlanLimitTest {
    @Test
    fun `smallest seeded category limit accommodates onboarding defaults`() {
        val categories =
            Path
                .of("../frontend/src/i18n/default-categories/en-US.yaml")
                .readText()
                .lineSequence()
                .count { it.startsWith("  ") && it.contains(":") }
        val migration = Path.of("src/main/resources/db/migration/V62__CreatePlanLimit.sql").readText()
        val seededUserLimit =
            Regex("'USER', 'USER', 'CATEGORIES', (\\d+)")
                .find(migration)
                ?.groupValues
                ?.get(1)
                ?.toInt()
                ?: error("USER category seed is missing")

        assertTrue(categories <= seededUserLimit, "$categories onboarding categories exceed the $seededUserLimit USER limit")
    }
}
