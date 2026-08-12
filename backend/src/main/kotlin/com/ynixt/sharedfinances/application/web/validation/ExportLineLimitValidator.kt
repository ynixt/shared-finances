package com.ynixt.sharedfinances.application.web.validation

import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import com.ynixt.sharedfinances.domain.exceptions.http.ExportLineLimitExceededException
import com.ynixt.sharedfinances.domain.services.plan.PlanLimitService
import org.springframework.stereotype.Component

@Component
class ExportLineLimitValidator(
    private val planLimitService: PlanLimitService,
) {
    suspend fun maximum(role: UserPlanRole): Int? = planLimitService.resolve(role, PlanLimitKey.EXPORT_MAX_LINES).value

    suspend fun validate(
        role: UserPlanRole,
        lineCount: Long,
    ) {
        maximum(role)?.let { maximum ->
            if (lineCount > maximum) throw ExportLineLimitExceededException(lineCount, maximum)
        }
    }
}
