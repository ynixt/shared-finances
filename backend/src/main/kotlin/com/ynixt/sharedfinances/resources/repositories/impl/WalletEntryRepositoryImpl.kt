package com.ynixt.sharedfinances.resources.repositories.impl

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySumResult
import com.ynixt.sharedfinances.domain.repositories.WalletEntryRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.WalletEntryR2DBCRepository
import com.ynixt.sharedfinances.resources.repositories.springdata.WalletEntrySpringDataRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.LocalDate
import java.util.UUID

@Repository
class WalletEntryRepositoryImpl(
    springDataRepository: WalletEntrySpringDataRepository,
    private val r2dbcRepository: WalletEntryR2DBCRepository,
) : EntityRepositoryImpl<WalletEntrySpringDataRepository, WalletEntryEntity>(springDataRepository),
    WalletEntryRepository {
    override fun sumForBankAccountSummary(
        userId: UUID?,
        groupId: UUID?,
        walletItemId: UUID?,
        minimumDate: LocalDate,
        maximumDate: LocalDate?,
    ): Flux<EntrySumResult> =
        r2dbcRepository.sumForBankAccountSummary(
            userId = userId,
            groupId = groupId,
            walletItemId = walletItemId,
            minimumDate = minimumDate,
            maximumDate = maximumDate,
        )
}
