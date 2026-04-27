package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.groups.debts.CreateGroupDebtAdjustmentRequestDto
import com.ynixt.sharedfinances.application.web.dto.groups.debts.EditGroupDebtAdjustmentRequestDto
import com.ynixt.sharedfinances.application.web.dto.groups.debts.GroupDebtMonthlyCompositionDto
import com.ynixt.sharedfinances.application.web.dto.groups.debts.GroupDebtMovementDto
import com.ynixt.sharedfinances.application.web.dto.groups.debts.GroupDebtPairBalanceDto
import com.ynixt.sharedfinances.application.web.dto.groups.debts.GroupDebtWorkspaceDto
import com.ynixt.sharedfinances.application.web.mapper.GroupDebtDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.WalletEventDtoMapper
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidGroupDebtAdjustmentException
import com.ynixt.sharedfinances.domain.models.groups.debts.EditGroupDebtManualAdjustmentInput
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtMovementLine
import com.ynixt.sharedfinances.domain.models.groups.debts.GroupDebtWorkspace
import com.ynixt.sharedfinances.domain.models.groups.debts.NewGroupDebtManualAdjustmentInput
import org.springframework.stereotype.Component
import java.time.YearMonth

@Component
class GroupDebtDtoMapperImpl(
    private val walletEventDtoMapper: WalletEventDtoMapper,
) : GroupDebtDtoMapper {
    override fun toWorkspaceDto(from: GroupDebtWorkspace): GroupDebtWorkspaceDto =
        GroupDebtWorkspaceDto(
            balances =
                from.balances.map { balance ->
                    GroupDebtPairBalanceDto(
                        payerId = balance.payerId,
                        receiverId = balance.receiverId,
                        currency = balance.currency,
                        outstandingAmount = balance.outstandingAmount,
                        monthlyComposition =
                            balance.monthlyComposition.map { month ->
                                GroupDebtMonthlyCompositionDto(
                                    month = month.month.toString(),
                                    netAmount = month.netAmount,
                                    chargeDelta = month.chargeDelta,
                                    settlementDelta = month.settlementDelta,
                                    manualAdjustmentDelta = month.manualAdjustmentDelta,
                                )
                            },
                    )
                },
        )

    override fun toMovementDto(from: GroupDebtMovementLine): GroupDebtMovementDto =
        GroupDebtMovementDto(
            id = from.id,
            payerId = from.payerId,
            receiverId = from.receiverId,
            month = from.month.toString(),
            currency = from.currency,
            deltaSigned = from.deltaSigned,
            reasonKind = from.reasonKind,
            createdByUserId = from.createdByUserId,
            note = from.note,
            sourceWalletEventId = from.sourceWalletEventId,
            sourceWalletEvent =
                from.sourceWalletEvent
                    ?.let(walletEventDtoMapper::fromListResponseToListDto),
            sourceMovementId = from.sourceMovementId,
            createdAt = from.createdAt,
        )

    override fun fromCreateAdjustmentRequestDto(from: CreateGroupDebtAdjustmentRequestDto): NewGroupDebtManualAdjustmentInput =
        NewGroupDebtManualAdjustmentInput(
            payerId = from.payerId,
            receiverId = from.receiverId,
            month = parseMonth(from.month),
            currency = from.currency,
            amountDelta = from.amountDelta,
            note = from.note,
        )

    override fun fromEditAdjustmentRequestDto(from: EditGroupDebtAdjustmentRequestDto): EditGroupDebtManualAdjustmentInput =
        EditGroupDebtManualAdjustmentInput(
            amountDelta = from.amountDelta,
            note = from.note,
        )

    private fun parseMonth(raw: String): YearMonth =
        runCatching { YearMonth.parse(raw) }
            .getOrElse {
                throw InvalidGroupDebtAdjustmentException("Month must use yyyy-MM format")
            }
}
