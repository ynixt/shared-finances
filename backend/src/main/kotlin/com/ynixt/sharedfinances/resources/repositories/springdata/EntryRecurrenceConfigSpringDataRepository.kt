package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.wallet.entries.EntryRecurrenceConfigEntity
import com.ynixt.sharedfinances.domain.repositories.EntityRepository
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

interface EntryRecurrenceConfigSpringDataRepository :
    Repository<EntryRecurrenceConfigEntity, String>,
    EntityRepository<EntryRecurrenceConfigEntity> {
    fun findAllByNextExecutionLessThanEqual(nextExecution: LocalDate): Flux<EntryRecurrenceConfigEntity>

    @Modifying
    @Query(
        """
            update entry_recurrence_config
            set
                last_execution = next_execution,
                next_execution = :nextExecution,
                qty_executed = qty_executed + 1,
                updated_at = CURRENT_TIMESTAMP
            where
                id = :id
                and next_execution = :oldNextExecution
        """,
    )
    fun updateConfigCausedByExecution(
        id: UUID,
        oldNextExecution: LocalDate,
        nextExecution: LocalDate?,
    ): Mono<Int>
}
