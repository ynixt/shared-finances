package com.ynixt.sharedfinances.application.web.dto.groups.debts

import com.ynixt.sharedfinances.application.web.dto.walletentry.EventForListDto
import com.ynixt.sharedfinances.domain.enums.GroupDebtMovementReasonKind
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class GroupDebtMovementDto(
    val id: UUID,
    val payerId: UUID,
    val receiverId: UUID,
    val month: String,
    val currency: String,
    val deltaSigned: BigDecimal,
    val reasonKind: GroupDebtMovementReasonKind,
    val createdByUserId: UUID,
    val note: String?,
    val sourceWalletEventId: UUID?,
    val sourceWalletEvent: EventForListDto? = null,
    val sourceMovementId: UUID?,
    val createdAt: OffsetDateTime?,
)
