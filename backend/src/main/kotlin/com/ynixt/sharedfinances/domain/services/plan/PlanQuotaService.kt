package com.ynixt.sharedfinances.domain.services.plan

import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import java.util.UUID

interface PlanQuotaService {
    suspend fun assertCanAdd(
        quotaOwnerUserId: UUID,
        quota: PlanLimitKey,
        requesterUserId: UUID = quotaOwnerUserId,
    )

    suspend fun currentUsage(
        userId: UUID,
        quota: PlanLimitKey,
    ): Long

    suspend fun usageChanged(
        userId: UUID,
        quota: PlanLimitKey,
    )

    suspend fun assertGroupCanAdd(
        groupId: UUID,
        quota: PlanLimitKey,
        requesterUserId: UUID,
        includeOutstandingInvitations: Boolean = false,
    ): Unit = Unit

    suspend fun currentGroupUsage(
        groupId: UUID,
        quota: PlanLimitKey,
        includeOutstandingInvitations: Boolean = false,
    ): Long = 0

    suspend fun groupUsageChanged(
        groupId: UUID,
        quota: PlanLimitKey,
        requesterUserId: UUID,
    ) = Unit
}
