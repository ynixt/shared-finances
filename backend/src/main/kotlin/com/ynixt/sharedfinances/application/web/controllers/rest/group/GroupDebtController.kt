package com.ynixt.sharedfinances.application.web.controllers.rest.group

import com.ynixt.sharedfinances.application.web.dto.groups.debts.CreateGroupDebtAdjustmentRequestDto
import com.ynixt.sharedfinances.application.web.dto.groups.debts.EditGroupDebtAdjustmentRequestDto
import com.ynixt.sharedfinances.application.web.dto.groups.debts.GroupDebtMovementDto
import com.ynixt.sharedfinances.application.web.dto.groups.debts.GroupDebtWorkspaceDto
import com.ynixt.sharedfinances.application.web.mapper.GroupDebtDtoMapper
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtHistoryFilter
import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.services.groups.GroupDebtService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/groups/{groupId}/debts")
@Tag(name = "Group debts", description = "Debt ledger workspace operations for a group")
class GroupDebtController(
    private val groupDebtService: GroupDebtService,
    private val groupDebtDtoMapper: GroupDebtDtoMapper,
) {
    @Operation(summary = "Get current group debt workspace")
    @GetMapping
    suspend fun getWorkspace(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
    ): GroupDebtWorkspaceDto =
        groupDebtDtoMapper.toWorkspaceDto(
            groupDebtService.getWorkspace(
                userId = principalToken.principal.id,
                groupId = groupId,
            ),
        )

    @Operation(summary = "Get chronological group debt movement history")
    @GetMapping("/history")
    suspend fun getHistory(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @RequestParam(required = false) payerId: UUID?,
        @RequestParam(required = false) receiverId: UUID?,
        @RequestParam(required = false) currency: String?,
    ): List<GroupDebtMovementDto> =
        groupDebtService
            .listHistory(
                userId = principalToken.principal.id,
                groupId = groupId,
                filter =
                    GroupDebtHistoryFilter(
                        payerId = payerId,
                        receiverId = receiverId,
                        currency = currency,
                    ),
            ).map(groupDebtDtoMapper::toMovementDto)

    @Operation(summary = "Get group debt movement by id")
    @GetMapping("/movements/{movementId}")
    suspend fun getMovement(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @PathVariable movementId: UUID,
    ): GroupDebtMovementDto =
        groupDebtDtoMapper.toMovementDto(
            groupDebtService.getMovement(
                userId = principalToken.principal.id,
                groupId = groupId,
                movementId = movementId,
            ),
        )

    @Operation(summary = "Create a manual group debt adjustment")
    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createAdjustment(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @RequestBody body: CreateGroupDebtAdjustmentRequestDto,
    ): GroupDebtMovementDto =
        groupDebtDtoMapper.toMovementDto(
            groupDebtService.createManualAdjustment(
                userId = principalToken.principal.id,
                groupId = groupId,
                input = groupDebtDtoMapper.fromCreateAdjustmentRequestDto(body),
            ),
        )

    @Operation(summary = "Edit a manual group debt adjustment by appending compensation")
    @PutMapping("/adjustments/{movementId}")
    suspend fun editAdjustment(
        @AuthenticationPrincipal principalToken: UserJwtAuthenticationToken,
        @PathVariable groupId: UUID,
        @PathVariable movementId: UUID,
        @RequestBody body: EditGroupDebtAdjustmentRequestDto,
    ): GroupDebtMovementDto =
        groupDebtDtoMapper.toMovementDto(
            groupDebtService.editManualAdjustment(
                userId = principalToken.principal.id,
                groupId = groupId,
                movementId = movementId,
                input = groupDebtDtoMapper.fromEditAdjustmentRequestDto(body),
            ),
        )
}
