package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySumResult
import reactor.core.publisher.Flux
import java.time.LocalDate
import java.util.UUID

interface WalletEntryRepository : EntityRepository<WalletEntryEntity> {
    fun sumForBankAccountSummary(
        userId: UUID?,
        groupId: UUID?,
        walletItemId: UUID?,
        minimumDate: LocalDate,
        maximumDate: LocalDate?,
    ): Flux<EntrySumResult>
}
