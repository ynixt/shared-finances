package com.ynixt.sharedfinances.domain.models.groups.debts

import com.ynixt.sharedfinances.domain.enums.GroupDebtMovementReasonKind
import com.ynixt.sharedfinances.domain.models.walletentry.EventListResponse
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.UUID

data class GroupDebtWorkspace(
    val balances: List<GroupDebtPairBalance>,
)

data class GroupDebtPairBalance(
    val payerId: UUID,
    val receiverId: UUID,
    val currency: String,
    val outstandingAmount: BigDecimal,
    val monthlyComposition: List<GroupDebtMonthlyComposition>,
)

data class GroupDebtMonthlyComposition(
    val month: YearMonth,
    val netAmount: BigDecimal,
    val chargeDelta: BigDecimal,
    val settlementDelta: BigDecimal,
    val manualAdjustmentDelta: BigDecimal,
)

data class GroupDebtMovementLine(
    val id: UUID,
    val payerId: UUID,
    val receiverId: UUID,
    val month: YearMonth,
    val currency: String,
    val deltaSigned: BigDecimal,
    val reasonKind: GroupDebtMovementReasonKind,
    val createdByUserId: UUID,
    val note: String?,
    val sourceWalletEventId: UUID?,
    val sourceWalletEvent: EventListResponse? = null,
    val sourceMovementId: UUID?,
    val createdAt: OffsetDateTime?,
)

data class NewGroupDebtManualAdjustmentInput(
    val payerId: UUID,
    val receiverId: UUID,
    val month: YearMonth,
    val currency: String,
    val amountDelta: BigDecimal,
    val note: String? = null,
)

data class EditGroupDebtManualAdjustmentInput(
    val amountDelta: BigDecimal,
    val note: String? = null,
)

data class GroupDebtHistoryFilter(
    val payerId: UUID? = null,
    val receiverId: UUID? = null,
    val currency: String? = null,
)

data class GroupDebtMonthlyCashFlow(
    val debtOutflow: BigDecimal,
    val debtInflow: BigDecimal,
)
