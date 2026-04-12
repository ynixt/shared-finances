package com.ynixt.sharedfinances.application.web.controllers.rest.group

import com.ynixt.sharedfinances.application.web.dto.goals.FinancialGoalSummaryDto
import com.ynixt.sharedfinances.application.web.dto.goals.NewFinancialGoalRequestDto
import com.ynixt.sharedfinances.application.web.mapper.FinancialGoalDtoMapper
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.goals.FinancialGoalManagementService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/groups/{groupId}/financial-goals")
@Tag(name = "Group financial goals", description = "Group-scoped goals collection operations")
class GroupFinancialGoalController(
    private val financialGoalManagementService: FinancialGoalManagementService,
    private val financialGoalDtoMapper: FinancialGoalDtoMapper,
) {
    @Operation(summary = "List goals from a specific group workspace")
    @GetMapping
    suspend fun listGroupGoals(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        pageable: Pageable,
    ): Page<FinancialGoalSummaryDto> =
        financialGoalManagementService
            .listGroupGoals(
                userId = principalToken.principal.id,
                groupId = groupId,
                pageable = pageable,
            ).map(financialGoalDtoMapper::toSummaryDto)

    @Operation(summary = "Create goal inside a specific group workspace")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createGroupGoal(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @RequestBody body: NewFinancialGoalRequestDto,
    ): FinancialGoalSummaryDto =
        financialGoalDtoMapper.toSummaryDto(
            financialGoalManagementService.createGoalForGroup(
                userId = principalToken.principal.id,
                groupId = groupId,
                input = financialGoalDtoMapper.fromNewRequestDto(body),
            ),
        )
}
