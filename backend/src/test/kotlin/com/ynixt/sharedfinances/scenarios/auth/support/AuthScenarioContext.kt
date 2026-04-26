package com.ynixt.sharedfinances.scenarios.auth.support

import com.ynixt.sharedfinances.domain.models.LoginResult
import com.ynixt.sharedfinances.scenarios.support.ScenarioContext
import java.util.UUID

internal class AuthScenarioContext(
    var currentEmail: String? = null,
    var currentPassword: String? = null,
    var currentSessionId: UUID? = null,
    var lastLoginResult: LoginResult? = null,
    var lastRegisterEmail: String? = null,
    var lastError: Throwable? = null,
) : ScenarioContext()
