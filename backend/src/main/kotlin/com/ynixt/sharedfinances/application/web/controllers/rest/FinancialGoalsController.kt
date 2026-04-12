package com.ynixt.sharedfinances.application.web.controllers.rest

import com.ynixt.sharedfinances.application.web.dto.goals.EditFinancialGoalRequestDto
import com.ynixt.sharedfinances.application.web.dto.goals.EditGoalLedgerMovementRequestDto
import com.ynixt.sharedfinances.application.web.dto.goals.EditGoalScheduleRequestDto
import com.ynixt.sharedfinances.application.web.dto.goals.FinancialGoalDetailDto
import com.ynixt.sharedfinances.application.web.dto.goals.FinancialGoalSummaryDto
import com.ynixt.sharedfinances.application.web.dto.goals.GoalAllocateRequestDto
import com.ynixt.sharedfinances.application.web.dto.goals.GoalContributionScheduleDto
import com.ynixt.sharedfinances.application.web.dto.goals.GoalLedgerMovementDto
import com.ynixt.sharedfinances.application.web.dto.goals.GoalReverseRequestDto
import com.ynixt.sharedfinances.application.web.dto.goals.NewFinancialGoalRequestDto
import com.ynixt.sharedfinances.application.web.dto.goals.NewGoalScheduleRequestDto
import com.ynixt.sharedfinances.application.web.mapper.FinancialGoalDtoMapper
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.goals.FinancialGoalManagementService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/financial-goals")
@Tag(name = "Financial goals", description = "Goals, ledger allocations, and contribution schedules")
class FinancialGoalsController(
    private val financialGoalManagementService: FinancialGoalManagementService,
    private val financialGoalDtoMapper: FinancialGoalDtoMapper,
) {
    @Operation(summary = "List individual goals visible to the user")
    @GetMapping
    suspend fun list(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        pageable: Pageable,
    ): Page<FinancialGoalSummaryDto> =
        financialGoalManagementService
            .listIndividualGoals(principalToken.principal.id, pageable)
            .map(financialGoalDtoMapper::toSummaryDto)

    @Operation(summary = "Get goal detail")
    @GetMapping("/{goalId}")
    suspend fun get(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable goalId: UUID,
    ): FinancialGoalDetailDto =
        financialGoalDtoMapper.toDetailDto(
            financialGoalManagementService.getGoalDetail(
                userId = principalToken.principal.id,
                goalId = goalId,
            ),
        )

    @Operation(summary = "Create goal")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun create(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @RequestBody body: NewFinancialGoalRequestDto,
    ): FinancialGoalSummaryDto =
        financialGoalDtoMapper.toSummaryDto(
            financialGoalManagementService.createGoal(
                userId = principalToken.principal.id,
                input = financialGoalDtoMapper.fromNewRequestDto(body),
            ),
        )

    @Operation(summary = "Update goal")
    @PatchMapping("/{goalId}")
    suspend fun update(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable goalId: UUID,
        @RequestBody body: EditFinancialGoalRequestDto,
    ): FinancialGoalSummaryDto =
        financialGoalDtoMapper.toSummaryDto(
            financialGoalManagementService.updateGoal(
                userId = principalToken.principal.id,
                goalId = goalId,
                input = financialGoalDtoMapper.fromEditRequestDto(body),
            ),
        )

    @Operation(summary = "Delete goal")
    @DeleteMapping("/{goalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun delete(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable goalId: UUID,
    ) {
        financialGoalManagementService.deleteGoal(
            userId = principalToken.principal.id,
            goalId = goalId,
        )
    }

    @Operation(summary = "Immediate allocation to goal")
    @PostMapping("/{goalId}/ledger/allocations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun allocate(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable goalId: UUID,
        @RequestBody body: GoalAllocateRequestDto,
    ) {
        financialGoalManagementService.allocateImmediate(
            userId = principalToken.principal.id,
            goalId = goalId,
            walletItemId = body.walletItemId,
            amount = body.amount,
            allocationDate = body.allocationDate,
            note = body.note,
        )
    }

    @Operation(summary = "Reverse allocation (creates opposite signed movement)")
    @PostMapping("/{goalId}/ledger/reversals")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun reverse(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable goalId: UUID,
        @RequestBody body: GoalReverseRequestDto,
    ) {
        financialGoalManagementService.reverseAllocation(
            userId = principalToken.principal.id,
            goalId = goalId,
            walletItemId = body.walletItemId,
            amount = body.amount,
            note = body.note,
        )
    }

    @Operation(summary = "Get ledger movement")
    @GetMapping("/{goalId}/ledger/movements/{movementId}")
    suspend fun getMovement(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable goalId: UUID,
        @PathVariable movementId: UUID,
    ): GoalLedgerMovementDto =
        financialGoalDtoMapper.toMovementDto(
            financialGoalManagementService.getLedgerMovement(
                userId = principalToken.principal.id,
                goalId = goalId,
                movementId = movementId,
            ),
        )

    @Operation(summary = "Edit ledger movement amount")
    @PatchMapping("/{goalId}/ledger/movements/{movementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun editMovement(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable goalId: UUID,
        @PathVariable movementId: UUID,
        @RequestBody body: EditGoalLedgerMovementRequestDto,
    ) {
        financialGoalManagementService.editLedgerMovement(
            userId = principalToken.principal.id,
            goalId = goalId,
            movementId = movementId,
            newSignedAmount = body.newSignedAmount,
            allocationDate = body.allocationDate,
            note = body.note,
        )
    }

    @Operation(summary = "Delete ledger movement")
    @DeleteMapping("/{goalId}/ledger/movements/{movementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun deleteMovement(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable goalId: UUID,
        @PathVariable movementId: UUID,
    ) {
        financialGoalManagementService.deleteLedgerMovement(
            userId = principalToken.principal.id,
            goalId = goalId,
            movementId = movementId,
        )
    }

    @Operation(summary = "List goal ledger movements")
    @GetMapping("/{goalId}/ledger/movements")
    suspend fun listMovements(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable goalId: UUID,
        pageable: Pageable,
    ): Page<GoalLedgerMovementDto> =
        financialGoalManagementService
            .listLedgerMovements(
                userId = principalToken.principal.id,
                goalId = goalId,
                pageable = pageable,
            ).map(financialGoalDtoMapper::toMovementDto)

    @Operation(summary = "Create scheduled contribution")
    @PostMapping("/{goalId}/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createSchedule(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable goalId: UUID,
        @RequestBody body: NewGoalScheduleRequestDto,
    ): GoalContributionScheduleDto =
        financialGoalDtoMapper.toContributionScheduleDto(
            financialGoalManagementService.createSchedule(
                userId = principalToken.principal.id,
                goalId = goalId,
                walletItemId = body.walletItemId,
                amount = body.amount,
                periodicity = body.periodicity,
                firstExecution = body.firstExecution,
                qtyLimit = body.qtyLimit,
                removesAllocation = body.removesAllocation,
            ),
        )

    @Operation(summary = "Get contribution schedule")
    @GetMapping("/{goalId}/schedules/{scheduleId}")
    suspend fun getSchedule(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable goalId: UUID,
        @PathVariable scheduleId: UUID,
    ): GoalContributionScheduleDto =
        financialGoalDtoMapper.toContributionScheduleDto(
            financialGoalManagementService.getSchedule(
                userId = principalToken.principal.id,
                goalId = goalId,
                scheduleId = scheduleId,
            ),
        )

    @Operation(summary = "Update contribution schedule")
    @PatchMapping("/{goalId}/schedules/{scheduleId}")
    suspend fun updateSchedule(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable goalId: UUID,
        @PathVariable scheduleId: UUID,
        @RequestBody body: EditGoalScheduleRequestDto,
    ): GoalContributionScheduleDto =
        financialGoalDtoMapper.toContributionScheduleDto(
            financialGoalManagementService.updateSchedule(
                userId = principalToken.principal.id,
                goalId = goalId,
                scheduleId = scheduleId,
                walletItemId = body.walletItemId,
                amount = body.amount,
                periodicity = body.periodicity,
                nextExecution = body.nextExecution,
                qtyLimit = body.qtyLimit,
                removesAllocation = body.removesAllocation,
            ),
        )

    @Operation(summary = "Delete scheduled contribution")
    @DeleteMapping("/{goalId}/schedules/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun deleteSchedule(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable goalId: UUID,
        @PathVariable scheduleId: UUID,
    ) {
        financialGoalManagementService.deleteSchedule(
            userId = principalToken.principal.id,
            goalId = goalId,
            scheduleId = scheduleId,
        )
    }

    @Operation(summary = "List contribution schedules")
    @GetMapping("/{goalId}/schedules")
    suspend fun listSchedules(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable goalId: UUID,
        pageable: Pageable,
    ): Page<GoalContributionScheduleDto> =
        financialGoalManagementService
            .listSchedules(
                userId = principalToken.principal.id,
                goalId = goalId,
                pageable = pageable,
            ).map(financialGoalDtoMapper::toContributionScheduleDto)
}
