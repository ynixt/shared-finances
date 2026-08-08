package com.ynixt.sharedfinances.application.config

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RuntimeLimitsConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(AuthConfiguration::class.java, ImportConfiguration::class.java)

    @Test
    fun `uses enabled registration and one thousand import lines by default`() {
        contextRunner.run { context ->
            assertEquals(true, context.getBean(AuthProperties::class.java).features.registrationEnabled)
            assertEquals(1000, context.getBean(ImportProperties::class.java).maxLines)
        }
    }

    @Test
    fun `binds custom runtime limits`() {
        contextRunner
            .withPropertyValues(
                "app.auth.features.registration-enabled=false",
                "app.imports.max-lines=2500",
            ).run { context ->
                assertEquals(false, context.getBean(AuthProperties::class.java).features.registrationEnabled)
                assertEquals(2500, context.getBean(ImportProperties::class.java).maxLines)
            }
    }

    @Test
    fun `rejects malformed registration setting`() {
        contextRunner
            .withPropertyValues("app.auth.features.registration-enabled=not-a-boolean")
            .run { context -> assertNotNull(context.startupFailure) }
    }

    @Test
    fun `rejects malformed import limit`() {
        contextRunner
            .withPropertyValues("app.imports.max-lines=not-an-integer")
            .run { context -> assertNotNull(context.startupFailure) }
    }

    @Test
    fun `rejects non-positive import limit`() {
        contextRunner
            .withPropertyValues("app.imports.max-lines=0")
            .run { context -> assertNotNull(context.startupFailure) }
    }
}
