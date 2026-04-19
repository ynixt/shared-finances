package com.ynixt.sharedfinances.resources.repositories.r2dbc.springdata

import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventBeneficiaryEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface WalletEventBeneficiarySpringDataRepository : R2dbcRepository<WalletEventBeneficiaryEntity, String> {
    fun findAllByWalletEventId(walletEventId: UUID): Flux<WalletEventBeneficiaryEntity>

    @Modifying
    @Query("DELETE FROM wallet_event_beneficiary WHERE wallet_event_id = :walletEventId")
    fun deleteAllByWalletEventId(walletEventId: UUID): Mono<Int>
}
