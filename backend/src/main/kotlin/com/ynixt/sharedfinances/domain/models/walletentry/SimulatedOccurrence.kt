package com.ynixt.sharedfinances.domain.models.walletentry

import java.time.LocalDate
import java.util.UUID

data class SimulatedOccurrence(
    val executionDate: LocalDate,
    val installment: Int?,
    val billDateByWalletItemId: Map<UUID, LocalDate?>,
)
