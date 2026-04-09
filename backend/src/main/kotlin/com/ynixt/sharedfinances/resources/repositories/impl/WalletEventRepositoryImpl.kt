package com.ynixt.sharedfinances.resources.repositories.impl

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.repositories.WalletEventCursorFindAll
import com.ynixt.sharedfinances.domain.repositories.WalletEventRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.WalletEventDatabaseClientRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.WalletEventSpringDataRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

@Repository
class WalletEventRepositoryImpl(
    private val springDataRepository: WalletEventSpringDataRepository,
    private val dcRepository: WalletEventDatabaseClientRepository,
) : WalletEventRepository {
    override fun findById(id: UUID): Mono<WalletEventEntity> = springDataRepository.findById(id.toString())

    override fun deleteById(id: UUID): Mono<Long> = springDataRepository.deleteById(id.toString()).thenReturn(1L)

    override fun findOneByRecurrenceEventIdAndDate(
        recurrenceEventId: UUID,
        date: LocalDate,
    ): Mono<WalletEventEntity> = springDataRepository.findOneByRecurrenceEventIdAndDate(recurrenceEventId, date)

    override fun findAllByRecurrenceEventId(recurrenceEventId: UUID): Flux<WalletEventEntity> =
        springDataRepository.findAllByRecurrenceEventId(recurrenceEventId)

    override fun save(walletEntry: WalletEventEntity): Mono<WalletEventEntity> = springDataRepository.save(walletEntry)

    override fun saveAll(walletEntry: Iterable<WalletEventEntity>): Flux<WalletEventEntity> = springDataRepository.saveAll(walletEntry)

    override fun deleteAllByWalletItemIdAndUserId(
        walletItemId: UUID,
        userId: UUID,
    ): Mono<Long> =
        springDataRepository.deleteAllByWalletItemIdAndUserId(
            walletItemId = walletItemId,
            userId = userId,
        )

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
        dcRepository.findAll(
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
