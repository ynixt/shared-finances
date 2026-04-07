package com.ynixt.sharedfinances.scenarios.user.support

import com.ynixt.sharedfinances.application.web.dto.auth.RegisterDto
import com.ynixt.sharedfinances.scenarios.support.ScenarioContext
import com.ynixt.sharedfinances.scenarios.support.ScenarioRuntime
import java.util.UUID

internal class UserScenarioSetupOps(
    private val runtime: ScenarioRuntime,
    private val context: ScenarioContext,
) {
    suspend fun createUser(
        email: String = "user-${UUID.randomUUID()}@example.com",
        password: String = "password123",
        firstName: String = "Scenario",
        lastName: String = "User",
        lang: String = "en",
        defaultCurrency: String = "USD",
        tmz: String = "UTC",
    ): UUID {
        val created =
            runtime.userService.createUser(
                RegisterDto(
                    email = email,
                    password = password,
                    firstName = firstName,
                    lastName = lastName,
                    lang = lang,
                    defaultCurrency = defaultCurrency,
                    tmz = tmz,
                ),
            )

        val userId = requireNotNull(created.id)
        context.currentUserId = userId
        context.currentCurrency = created.defaultCurrency
        return userId
    }
}
