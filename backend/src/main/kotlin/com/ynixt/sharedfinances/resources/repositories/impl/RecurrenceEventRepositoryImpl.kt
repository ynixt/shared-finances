package com.ynixt.sharedfinances.resources.repositories.impl

import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEventRepository
import com.ynixt.sharedfinances.domain.repositories.WalletTransactionQueryScope
import com.ynixt.sharedfinances.resources.repositories.r2dbc.databaseclient.RecurrenceEventDatabaseClientRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata.RecurrenceEventSpringDataRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

@Repository
class RecurrenceEventRepositoryImpl(
    springDataRepository: RecurrenceEventSpringDataRepository,
    private val dcRepository: RecurrenceEventDatabaseClientRepository,
) : EntityRepositoryImpl<RecurrenceEventSpringDataRepository, RecurrenceEventEntity>(
        springDataRepository,
    ),
    RecurrenceEventRepository {
    override fun findAllByNextExecutionLessThanEqual(nextExecution: LocalDate): Flux<RecurrenceEventEntity> =
        springDataRepository.findAllByNextExecutionLessThanEqual(nextExecution)

    override fun findAllBySeriesId(seriesId: UUID): Flux<RecurrenceEventEntity> = springDataRepository.findAllBySeriesId(seriesId)

    override fun deleteAllByWalletItemIdAndUserId(
        walletItemId: UUID,
        userId: UUID,
    ): Mono<Long> =
        springDataRepository.deleteAllByWalletItemIdAndUserId(
            walletItemId = walletItemId,
            userId = userId,
        )

    override fun deleteAllByGroupIdAndUserId(
        groupId: UUID,
        userId: UUID,
    ): Mono<Long> = springDataRepository.deleteAllByGroupIdAndUserId(groupId, userId)

    override fun deleteAllForAccountDeletion(userId: UUID): Mono<Long> = springDataRepository.deleteAllForAccountDeletion(userId)

    override fun updateConfigCausedByExecution(
        id: UUID,
        oldNextExecution: LocalDate,
        nextExecution: LocalDate?,
    ): Mono<Int> =
        springDataRepository.updateConfigCausedByExecution(
            id = id,
            oldNextExecution = oldNextExecution,
            nextExecution = nextExecution,
        )

    override fun findAllEntries(
        scope: WalletTransactionQueryScope,
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        billDate: LocalDate?,
        walletItemId: UUID?,
        walletItemIds: Set<UUID>,
        entryTypes: Set<WalletEntryType>,
        sort: Sort,
    ): Flux<RecurrenceEventEntity> =
        dcRepository.findAllEntries(
            scope = scope,
            minimumEndExecution = minimumEndExecution,
            maximumNextExecution = maximumNextExecution,
            billDate = billDate,
            walletItemId = walletItemId,
            walletItemIds = walletItemIds,
            entryTypes = entryTypes,
            sort = sort,
        )
}
