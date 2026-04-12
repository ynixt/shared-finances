package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.goals.EditFinancialGoalRequestDto
import com.ynixt.sharedfinances.application.web.dto.goals.FinancialGoalDetailDto
import com.ynixt.sharedfinances.application.web.dto.goals.FinancialGoalSummaryDto
import com.ynixt.sharedfinances.application.web.dto.goals.FinancialGoalTargetDto
import com.ynixt.sharedfinances.application.web.dto.goals.GoalCommitmentChartSeriesDto
import com.ynixt.sharedfinances.application.web.dto.goals.GoalCommitmentMonthlyPointDto
import com.ynixt.sharedfinances.application.web.dto.goals.GoalContributionScheduleDto
import com.ynixt.sharedfinances.application.web.dto.goals.GoalLedgerMovementDto
import com.ynixt.sharedfinances.application.web.dto.goals.NewFinancialGoalRequestDto
import com.ynixt.sharedfinances.application.web.mapper.FinancialGoalDtoMapper
import com.ynixt.sharedfinances.domain.models.goals.FinancialGoalDetail
import com.ynixt.sharedfinances.domain.models.goals.FinancialGoalHeader
import com.ynixt.sharedfinances.domain.models.goals.FinancialGoalTargetAmount
import com.ynixt.sharedfinances.domain.models.goals.GoalCommitmentChartSeries
import com.ynixt.sharedfinances.domain.models.goals.GoalCommitmentMonthlyPoint
import com.ynixt.sharedfinances.domain.models.goals.GoalContributionScheduleLine
import com.ynixt.sharedfinances.domain.models.goals.GoalLedgerMovementLine
import com.ynixt.sharedfinances.domain.services.goals.EditFinancialGoalInput
import com.ynixt.sharedfinances.domain.services.goals.NewFinancialGoalInput
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie
import tech.mappie.api.builtin.collections.IterableToListMapper

@Component
class FinancialGoalDtoMapperImpl : FinancialGoalDtoMapper {
    override fun toSummaryDto(from: FinancialGoalHeader): FinancialGoalSummaryDto = HeaderToSummaryDtoMapper.map(from)

    override fun toDetailDto(from: FinancialGoalDetail): FinancialGoalDetailDto = DetailToDtoMapper.map(from)

    override fun toContributionScheduleDto(from: GoalContributionScheduleLine): GoalContributionScheduleDto =
        ScheduleLineToDtoMapper.map(from)

    override fun toMovementDto(from: GoalLedgerMovementLine): GoalLedgerMovementDto = MovementLineToDtoMapper.map(from)

    override fun fromNewRequestDto(from: NewFinancialGoalRequestDto): NewFinancialGoalInput = NewRequestToInputMapper.map(from)

    override fun fromEditRequestDto(from: EditFinancialGoalRequestDto): EditFinancialGoalInput = EditRequestToInputMapper.map(from)

    private object HeaderToSummaryDtoMapper : ObjectMappie<FinancialGoalHeader, FinancialGoalSummaryDto>() {
        override fun map(from: FinancialGoalHeader) = mapping {}
    }

    private object TargetAmountToDtoMapper : ObjectMappie<FinancialGoalTargetAmount, FinancialGoalTargetDto>() {
        override fun map(from: FinancialGoalTargetAmount) = mapping {}
    }

    private object MovementLineToDtoMapper : ObjectMappie<GoalLedgerMovementLine, GoalLedgerMovementDto>() {
        override fun map(from: GoalLedgerMovementLine) = mapping {}
    }

    private object ScheduleLineToDtoMapper : ObjectMappie<GoalContributionScheduleLine, GoalContributionScheduleDto>() {
        override fun map(from: GoalContributionScheduleLine) = mapping {}
    }

    private object CommitmentMonthlyPointToDtoMapper : ObjectMappie<GoalCommitmentMonthlyPoint, GoalCommitmentMonthlyPointDto>() {
        override fun map(from: GoalCommitmentMonthlyPoint) = mapping {}
    }

    private object CommitmentSeriesToDtoMapper : ObjectMappie<GoalCommitmentChartSeries, GoalCommitmentChartSeriesDto>() {
        override fun map(from: GoalCommitmentChartSeries) =
            mapping {
                to::points fromProperty from::points via IterableToListMapper(CommitmentMonthlyPointToDtoMapper)
            }
    }

    private object DetailToDtoMapper : ObjectMappie<FinancialGoalDetail, FinancialGoalDetailDto>() {
        override fun map(from: FinancialGoalDetail) =
            mapping {
                to::goal fromProperty from::goal via HeaderToSummaryDtoMapper
                to::targets fromProperty from::targets via IterableToListMapper(TargetAmountToDtoMapper)
                to::committedByCurrency fromProperty from::committedByCurrency
                to::commitmentChart fromProperty from::commitmentChart via IterableToListMapper(CommitmentSeriesToDtoMapper)
            }
    }

    private object TargetDtoToAmountMapper : ObjectMappie<FinancialGoalTargetDto, FinancialGoalTargetAmount>() {
        override fun map(from: FinancialGoalTargetDto) = mapping {}
    }

    private object NewRequestToInputMapper : ObjectMappie<NewFinancialGoalRequestDto, NewFinancialGoalInput>() {
        override fun map(from: NewFinancialGoalRequestDto) =
            mapping {
                to::targets fromProperty from::targets via IterableToListMapper(TargetDtoToAmountMapper)
            }
    }

    private object EditRequestToInputMapper : ObjectMappie<EditFinancialGoalRequestDto, EditFinancialGoalInput>() {
        override fun map(from: EditFinancialGoalRequestDto) =
            mapping {
                to::targets fromProperty from::targets via IterableToListMapper(TargetDtoToAmountMapper)
            }
    }
}
