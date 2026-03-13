package com.ynixt.sharedfinances.resources.repositories.springdata

import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEntryEntity
import com.ynixt.sharedfinances.domain.repositories.EntityRepository
import com.ynixt.sharedfinances.domain.repositories.RecurrenceEntryRepository
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.Repository
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

interface RecurrenceEntrySpringDataRepository :
    Repository<RecurrenceEntryEntity, String>,
    EntityRepository<RecurrenceEntryEntity>,
    RecurrenceEntryRepository {
    @Modifying
    @Query(
        """
            update recurrence_entry
            set
                next_bill_date = :nextBillDate,
                updated_at = CURRENT_TIMESTAMP
            where
                id = :id
        """,
    )
    override fun updateNextBillDate(
        id: UUID,
        nextBillDate: LocalDate?,
    ): Mono<Int>
}
