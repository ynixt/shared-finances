package com.ynixt.sharedfinances.resources.repositories.impl

import com.ynixt.sharedfinances.domain.entities.wallet.entries.EntryRecurrenceConfigEntity
import com.ynixt.sharedfinances.domain.repositories.EntryRecurrenceConfigRepository
import com.ynixt.sharedfinances.resources.repositories.r2dbc.EntryRecurrenceConfigR2DBCRepository
import com.ynixt.sharedfinances.resources.repositories.springdata.EntryRecurrenceConfigSpringDataRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

@Repository
class EntryRecurrenceConfigRepositoryImpl(
    springDataRepository: EntryRecurrenceConfigSpringDataRepository,
    private val r2dbcRepository: EntryRecurrenceConfigR2DBCRepository,
) : EntityRepositoryImpl<EntryRecurrenceConfigSpringDataRepository, EntryRecurrenceConfigEntity>(
        springDataRepository,
    ),
    EntryRecurrenceConfigRepository {
    override fun findAllByNextExecutionLessThanEqual(nextExecution: LocalDate): Flux<EntryRecurrenceConfigEntity> =
        springDataRepository.findAllByNextExecutionLessThanEqual(nextExecution)

    override fun updateConfigCausedByExecution(
        id: UUID,
        oldNextExecution: LocalDate,
        nextExecution: LocalDate?,
        nextOriginBillDate: LocalDate?,
        nextTargetBillDate: LocalDate?,
    ): Mono<Int> =
        springDataRepository.updateConfigCausedByExecution(
            id = id,
            oldNextExecution = oldNextExecution,
            nextExecution = nextExecution,
            nextOriginBillDate = nextOriginBillDate,
            nextTargetBillDate = nextTargetBillDate,
        )

    override fun findAll(
        minimumEndExecution: LocalDate?,
        maximumNextExecution: LocalDate?,
        billDate: LocalDate?,
        originId: UUID?,
        targetId: UUID?,
        userId: UUID?,
        groupId: UUID?,
        sort: Sort,
    ): Flux<EntryRecurrenceConfigEntity> =
        r2dbcRepository.findAll(
            minimumEndExecution = minimumEndExecution,
            maximumNextExecution = maximumNextExecution,
            billDate = billDate,
            originId = originId,
            targetId = targetId,
            userId = userId,
            groupId = groupId,
            sort = sort,
        )
}
