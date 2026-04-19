package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.groups.debts.CreateGroupDebtAdjustmentRequestDto
import com.ynixt.sharedfinances.application.web.dto.groups.debts.EditGroupDebtAdjustmentRequestDto
import com.ynixt.sharedfinances.application.web.dto.groups.debts.GroupDebtMovementDto
import com.ynixt.sharedfinances.application.web.dto.groups.debts.GroupDebtWorkspaceDto
import com.ynixt.sharedfinances.domain.models.groups.debts.EditGroupDebtManualAdjustmentInput
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtMovementLine
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtWorkspace
import com.ynixt.sharedfinances.domain.models.groups.debts.NewGroupDebtManualAdjustmentInput

interface GroupDebtDtoMapper {
    fun toWorkspaceDto(from: GroupDebtWorkspace): GroupDebtWorkspaceDto

    fun toMovementDto(from: GroupDebtMovementLine): GroupDebtMovementDto

    fun fromCreateAdjustmentRequestDto(from: CreateGroupDebtAdjustmentRequestDto): NewGroupDebtManualAdjustmentInput

    fun fromEditAdjustmentRequestDto(from: EditGroupDebtAdjustmentRequestDto): EditGroupDebtManualAdjustmentInput
}
