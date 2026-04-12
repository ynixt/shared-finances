package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.goals.EditFinancialGoalRequestDto
import com.ynixt.sharedfinances.application.web.dto.goals.FinancialGoalDetailDto
import com.ynixt.sharedfinances.application.web.dto.goals.FinancialGoalSummaryDto
import com.ynixt.sharedfinances.application.web.dto.goals.GoalContributionScheduleDto
import com.ynixt.sharedfinances.application.web.dto.goals.GoalLedgerMovementDto
import com.ynixt.sharedfinances.application.web.dto.goals.NewFinancialGoalRequestDto
import com.ynixt.sharedfinances.domain.models.goals.FinancialGoalDetail
import com.ynixt.sharedfinances.domain.models.goals.FinancialGoalHeader
import com.ynixt.sharedfinances.domain.models.goals.GoalContributionScheduleLine
import com.ynixt.sharedfinances.domain.models.goals.GoalLedgerMovementLine
import com.ynixt.sharedfinances.domain.services.goals.EditFinancialGoalInput
import com.ynixt.sharedfinances.domain.services.goals.NewFinancialGoalInput

interface FinancialGoalDtoMapper {
    fun toSummaryDto(from: FinancialGoalHeader): FinancialGoalSummaryDto

    fun toDetailDto(from: FinancialGoalDetail): FinancialGoalDetailDto

    fun toContributionScheduleDto(from: GoalContributionScheduleLine): GoalContributionScheduleDto

    fun toMovementDto(from: GoalLedgerMovementLine): GoalLedgerMovementDto

    fun fromNewRequestDto(from: NewFinancialGoalRequestDto): NewFinancialGoalInput

    fun fromEditRequestDto(from: EditFinancialGoalRequestDto): EditFinancialGoalInput
}
