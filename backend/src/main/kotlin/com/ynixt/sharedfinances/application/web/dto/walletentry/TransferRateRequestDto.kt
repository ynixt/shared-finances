package com.ynixt.sharedfinances.application.web.dto.walletentry

import java.time.LocalDate
import java.util.UUID

data class TransferRateRequestDto(
    val groupId: UUID?,
    val originId: UUID,
    val targetId: UUID,
    val date: LocalDate,
)
