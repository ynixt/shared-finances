package com.ynixt.sharedfinances.application.web.validation

import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import com.ynixt.sharedfinances.domain.exceptions.http.ImportLineLimitExceededException
import com.ynixt.sharedfinances.domain.services.plan.PlanLimitService
import org.springframework.stereotype.Component

@Component
class ImportLineLimitValidator(
    private val planLimitService: PlanLimitService,
) {
    suspend fun maximum(role: UserPlanRole): Int? = planLimitService.resolve(role, PlanLimitKey.IMPORT_MAX_LINES).value

    suspend fun validate(
        role: UserPlanRole,
        lineCount: Int,
    ) {
        val maximum = maximum(role)
        if (maximum != null && lineCount > maximum) {
            throw ImportLineLimitExceededException(maximum)
        }
    }
}
