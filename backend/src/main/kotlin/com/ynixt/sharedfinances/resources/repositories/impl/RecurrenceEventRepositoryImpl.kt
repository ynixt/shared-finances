package com.ynixt.sharedfinances.resources.repositories.impl

import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEventRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.RecurrenceEventR2DBCRepository
import com.ynixt.sharedfinances.resources.repositories.springdata.RecurrenceEventSpringDataRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

@Repository
class RecurrenceEventRepositoryImpl(
    springDataRepository: RecurrenceEventSpringDataRepository,
    private val r2dbcRepository: RecurrenceEventR2DBCRepository,
) : EntityRepositoryImpl<RecurrenceEventSpringDataRepository, RecurrenceEventEntity>(
        springDataRepository,
    ),
    RecurrenceEventRepository {
    override fun findAllByNextExecutionLessThanEqual(nextExecution: LocalDate): Flux<RecurrenceEventEntity> =
        springDataRepository.findAllByNextExecutionLessThanEqual(nextExecution)

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

    override fun findAll(
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        billDate: LocalDate?,
        walletItemId: UUID?,
        userId: UUID?,
        groupId: UUID?,
        sort: Sort,
    ): Flux<RecurrenceEventEntity> =
        r2dbcRepository.findAll(
            minimumEndExecution = minimumEndExecution,
            maximumNextExecution = maximumNextExecution,
            billDate = billDate,
            walletItemId = walletItemId,
            userId = userId,
            groupId = groupId,
            sort = sort,
        )
}
