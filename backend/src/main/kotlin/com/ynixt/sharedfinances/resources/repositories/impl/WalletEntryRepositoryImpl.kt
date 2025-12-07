package com.ynixt.sharedfinances.resources.repositories.impl

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.models.walletentry.EntrySumResult
import com.ynixt.sharedfinances.domain.repositories.WalletEntryCursorFindAll
import com.ynixt.sharedfinances.domain.repositories.WalletEntryRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.WalletEntryR2DBCRepository
import com.ynixt.sharedfinances.resources.repositories.springdata.WalletEntrySpringDataRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

@Repository
class WalletEntryRepositoryImpl(
    private val springDataRepository: WalletEntrySpringDataRepository,
    private val r2dbcRepository: WalletEntryR2DBCRepository,
) : WalletEntryRepository {
    override fun save(walletEntry: WalletEntryEntity): Mono<WalletEntryEntity> = springDataRepository.save(walletEntry)

    override fun saveAll(walletEntry: Iterable<WalletEntryEntity>): Flux<WalletEntryEntity> = springDataRepository.saveAll(walletEntry)

    override fun findAll(
        userId: UUID?,
        groupId: UUID?,
        limit: Int,
        walletItemId: UUID?,
        minimumDate: LocalDate?,
        maximumDate: LocalDate?,
        billId: UUID?,
        cursor: WalletEntryCursorFindAll?,
    ): Flux<WalletEntryEntity> =
        r2dbcRepository.findAll(
            userId = userId,
            groupId = groupId,
            limit = limit,
            walletItemId = walletItemId,
            minimumDate = minimumDate,
            maximumDate = maximumDate,
            billId = billId,
            cursor = cursor,
        )

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
