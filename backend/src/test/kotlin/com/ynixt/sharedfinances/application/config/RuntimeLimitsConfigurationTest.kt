package com.ynixt.sharedfinances.application.config

import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RuntimeLimitsConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(AuthConfiguration::class.java, PlanConfiguration::class.java)

    @Test
    fun `uses enabled registration and disabled plan limits by default`() {
        contextRunner.run { context ->
            assertEquals(true, context.getBean(AuthProperties::class.java).features.registrationEnabled)
            assertEquals(false, context.getBean(PlanProperties::class.java).enabled)
            assertEquals(UserPlanRole.PRO, context.getBean(PlanProperties::class.java).defaultRole)
        }
    }

    @Test
    fun `binds custom runtime limits`() {
        contextRunner
            .withPropertyValues(
                "app.auth.features.registration-enabled=false",
                "app.plan.enabled=true",
                "app.plan.default-role=user",
            ).run { context ->
                assertEquals(false, context.getBean(AuthProperties::class.java).features.registrationEnabled)
                assertEquals(true, context.getBean(PlanProperties::class.java).enabled)
                assertEquals(UserPlanRole.USER, context.getBean(PlanProperties::class.java).defaultRole)
            }
    }

    @Test
    fun `rejects malformed registration setting`() {
        contextRunner
            .withPropertyValues("app.auth.features.registration-enabled=not-a-boolean")
            .run { context -> assertNotNull(context.startupFailure) }
    }

    @Test
    fun `rejects malformed plan switch`() {
        contextRunner
            .withPropertyValues("app.plan.enabled=not-a-boolean")
            .run { context -> assertNotNull(context.startupFailure) }
    }

    @Test
    fun `rejects unknown default plan role`() {
        contextRunner
            .withPropertyValues("app.plan.default-role=unknown")
            .run { context -> assertNotNull(context.startupFailure) }
    }
}
