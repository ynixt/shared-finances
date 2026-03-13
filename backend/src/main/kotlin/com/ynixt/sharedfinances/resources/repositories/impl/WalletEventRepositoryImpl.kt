package com.ynixt.sharedfinances.resources.repositories.impl

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.repositories.WalletEventCursorFindAll
import com.ynixt.sharedfinances.domain.repositories.WalletEventRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.WalletEventR2DBCRepository
import com.ynixt.sharedfinances.resources.repositories.springdata.WalletEventSpringDataRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

@Repository
class WalletEventRepositoryImpl(
    private val springDataRepository: WalletEventSpringDataRepository,
    private val r2dbcRepository: WalletEventR2DBCRepository,
) : WalletEventRepository {
    override fun save(walletEntry: WalletEventEntity): Mono<WalletEventEntity> = springDataRepository.save(walletEntry)

    override fun saveAll(walletEntry: Iterable<WalletEventEntity>): Flux<WalletEventEntity> = springDataRepository.saveAll(walletEntry)

    override fun findAll(
        userId: UUID?,
        groupId: UUID?,
        limit: Int,
        walletItemId: UUID?,
        minimumDate: LocalDate?,
        maximumDate: LocalDate?,
        billId: UUID?,
        cursor: WalletEventCursorFindAll?,
    ): Flux<WalletEventEntity> =
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
}
