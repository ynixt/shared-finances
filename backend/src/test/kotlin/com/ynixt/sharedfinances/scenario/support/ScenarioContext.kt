package com.ynixt.sharedfinances.scenario.support

import java.util.UUID

internal open class ScenarioContext(
    var currentUserId: UUID? = null,
    var currentCurrency: String = "USD",
)
