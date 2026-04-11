package com.ynixt.sharedfinances.application.web.dto.walletentry

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class TransferQuoteRequestDto(
    val groupId: UUID?,
    val originId: UUID,
    val targetId: UUID,
    val date: LocalDate,
    val originValue: BigDecimal,
)
