package com.ynixt.sharedfinances.scenarios.support

import java.util.UUID

internal open class ScenarioContext(
    var currentUserId: UUID? = null,
    var currentCurrency: String = "USD",
)
