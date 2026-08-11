package com.ynixt.sharedfinances.domain.exceptions.http

import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import org.springframework.http.HttpStatus
import java.util.UUID

class PlanQuotaExceededException(
    val quota: PlanLimitKey,
    val quotaOwnerUserId: UUID? = null,
    val groupId: UUID? = null,
) : AppResponseException(
        statusCode = HttpStatus.CONFLICT,
        errorCode = ERROR_CODE,
        messageI18n = "apiErrors.plan.quotaExceeded",
        argsI18n =
            buildMap {
                put("quota", quota.name)
                quotaOwnerUserId?.let { put("quotaOwnerUserId", it) }
                groupId?.let { put("groupId", it) }
            },
        alternativeMessage =
            if (groupId != null) {
                "The ${quota.name} plan quota for group $groupId has been reached."
            } else if (quotaOwnerUserId == null) {
                "The ${quota.name} plan quota has been reached."
            } else {
                "The ${quota.name} plan quota for user $quotaOwnerUserId has been reached."
            },
    ) {
    companion object {
        const val ERROR_CODE = "PLAN_QUOTA_EXCEEDED"
    }
}
