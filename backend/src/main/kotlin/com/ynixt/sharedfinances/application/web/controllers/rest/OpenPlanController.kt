package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.config.PlanProperties
import com.ynixt.sharedfinances.application.web.dto.plan.PublishedGroupTierDto
import com.ynixt.sharedfinances.application.web.dto.plan.PublishedInactivityPolicyDto
import com.ynixt.sharedfinances.application.web.dto.plan.PublishedPlanComparisonDto
import com.ynixt.sharedfinances.application.web.dto.plan.PublishedPlanLimitDto
import com.ynixt.sharedfinances.application.web.dto.plan.PublishedUserPlanDto
import com.ynixt.sharedfinances.domain.enums.GroupPlanTier
import com.ynixt.sharedfinances.domain.enums.PlanLimitKey
import com.ynixt.sharedfinances.domain.enums.PlanLimitScope
import com.ynixt.sharedfinances.domain.enums.UserPlanRole
import com.ynixt.sharedfinances.domain.exceptions.http.PlanComparisonUnavailableException
import com.ynixt.sharedfinances.domain.models.plan.ResolvedPlanLimit
import com.ynixt.sharedfinances.domain.services.plan.PlanLimitService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/open/plans")
@Tag(name = "Plans", description = "Public plan and group-tier comparison")
class OpenPlanController(
    private val planLimitService: PlanLimitService,
    private val planProperties: PlanProperties,
) {
    @GetMapping
    @Operation(summary = "List the limits currently enforced for public plans and group tiers")
    suspend fun comparison(): ResponseEntity<PublishedPlanComparisonDto> {
        if (!planProperties.enabled) throw PlanComparisonUnavailableException()

        return ResponseEntity.ok(
            PublishedPlanComparisonDto(
                userPlans =
                    UserPlanRole.entries
                        .filterNot { it == UserPlanRole.ADMINISTRATOR }
                        .map { userPlan(it) },
                groupTiers = GroupPlanTier.entries.map { groupTier(it) },
            ),
        )
    }

    private suspend fun userPlan(plan: UserPlanRole): PublishedUserPlanDto {
        val retention = planLimitService.resolve(plan, PlanLimitKey.INACTIVITY_RETENTION_MONTHS)
        return PublishedUserPlanDto(
            plan = plan,
            limits =
                PlanLimitKey.entries
                    .filter { it.scope == PlanLimitScope.USER && it != PlanLimitKey.INACTIVITY_RETENTION_MONTHS }
                    .map { quota -> published(planLimitService.resolve(plan, quota), quota) },
            inactivityPolicy = PublishedInactivityPolicyDto(retention.value, retention.unlimited),
        )
    }

    private suspend fun groupTier(tier: GroupPlanTier) =
        PublishedGroupTierDto(
            tier = tier,
            limits =
                PlanLimitKey.entries
                    .filter { it.scope == PlanLimitScope.GROUP }
                    .map { quota -> published(planLimitService.resolve(tier, quota), quota) },
        )

    private fun published(
        resolved: ResolvedPlanLimit,
        quota: PlanLimitKey,
    ) = PublishedPlanLimitDto(quota = quota, limit = resolved.value, unlimited = resolved.unlimited)
}
