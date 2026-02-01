package com.ynixt.sharedfinances.domain.services.walletentry

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

interface EntryRecurrenceConfigService {
    suspend fun getFutureValuesOfWalletItem(
        walletIdId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BigDecimal
}
