package com.ynixt.sharedfinances.resources.services.plan

import com.ynixt.sharedfinances.application.config.InactiveAccountDeletionProperties
import com.ynixt.sharedfinances.application.config.PlanProperties
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import com.ynixt.sharedfinances.domain.models.plan.PlanQuotaEntitlement
import com.ynixt.sharedfinances.domain.models.plan.UserEntitlements
import com.ynixt.sharedfinances.domain.services.plan.PlanEntitlementsService
import com.ynixt.sharedfinances.domain.services.plan.PlanLimitService
import com.ynixt.sharedfinances.domain.services.plan.PlanQuotaService
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.util.UUID

@Service
class PlanEntitlementsServiceImpl(
    private val limitService: PlanLimitService,
    private val quotaService: PlanQuotaService,
    private val clock: Clock,
    private val inactiveAccountDeletionProperties: InactiveAccountDeletionProperties,
    private val planProperties: PlanProperties,
) : PlanEntitlementsService {
    override suspend fun get(
        userId: UUID,
        role: UserPlanRole,
        lastLoginAt: OffsetDateTime,
    ): UserEntitlements {
        val retention = limitService.resolve(role, PlanLimitKey.INACTIVITY_RETENTION_MONTHS)
        return UserEntitlements(
            limitsEnabled = planProperties.enabled,
            role = role,
            importMaxLines = limitService.resolve(role, PlanLimitKey.IMPORT_MAX_LINES).value,
            quotas =
                PlanLimitKey.entries
                    .filter {
                        it.scope == com.ynixt.sharedfinances.domain.enums.PlanLimitScope.USER && it.countableQuota
                    }.map { quota ->
                        val limit = limitService.resolve(role, quota)
                        PlanQuotaEntitlement(
                            quota = quota,
                            limit = limit.value,
                            usage = quotaService.currentUsage(userId, quota),
                            unlimited = limit.unlimited,
                            windowEnd = nextUtcMonthStart().takeIf { quota.monthly },
                        )
                    },
            projectedDeletionAt =
                retention.value
                    ?.takeIf { planProperties.enabled && inactiveAccountDeletionProperties.enabled }
                    ?.let { lastLoginAt.plusMonths(it.toLong()).toInstant() },
        )
    }

    private fun nextUtcMonthStart() =
        clock
            .instant()
            .atZone(ZoneOffset.UTC)
            .with(TemporalAdjusters.firstDayOfNextMonth())
            .toLocalDate()
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
}
