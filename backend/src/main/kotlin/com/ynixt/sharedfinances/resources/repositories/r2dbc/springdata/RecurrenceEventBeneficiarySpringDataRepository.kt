package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventBeneficiaryEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface RecurrenceEventBeneficiarySpringDataRepository : R2dbcRepository<RecurrenceEventBeneficiaryEntity, String> {
    fun findAllByWalletEventId(walletEventId: UUID): Flux<RecurrenceEventBeneficiaryEntity>

    @Modifying
    @Query("DELETE FROM recurrence_event_beneficiary WHERE wallet_event_id = :walletEventId")
    fun deleteAllByWalletEventId(walletEventId: UUID): Mono<Int>
}
