package com.ynixt.sharedfinances.domain.entities

import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.PlanLimitScope
import com.ynixt.sharedfinances.domain.enums.UserPlanRole

data class PlanLimitEntity(
    val scope: PlanLimitScope,
    val planKey: String,
    val limitKey: PlanLimitKey,
    val limitValue: Int?,
) {
    constructor(
        scope: PlanLimitScope,
        planKey: UserPlanRole,
        limitKey: PlanLimitKey,
        limitValue: Int?,
    ) : this(scope, planKey.name, limitKey, limitValue)
}
