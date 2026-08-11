package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import java.time.Instant
import java.util.UUID

interface PlanQuotaUsageRepository {
    suspend fun acquireTransactionLock(
        userId: UUID,
        quota: PlanLimitKey,
    )

    suspend fun countUsage(
        userId: UUID,
        quota: PlanLimitKey,
        utcMonthStart: Instant,
    ): Long

    suspend fun acquireGroupTransactionLock(
        groupId: UUID,
        quota: PlanLimitKey,
    ): Unit = throw UnsupportedOperationException("Group quotas are not supported")

    suspend fun countGroupUsage(
        groupId: UUID,
        quota: PlanLimitKey,
        includeOutstandingInvitations: Boolean = false,
    ): Long = throw UnsupportedOperationException("Group quotas are not supported")
}
