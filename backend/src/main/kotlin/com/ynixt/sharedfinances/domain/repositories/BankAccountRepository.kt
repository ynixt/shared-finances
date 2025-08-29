package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.BankAccount
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface BankAccountRepository {
    fun findAllByUserId(
        userId: UUID,
        pageable: Pageable,
    ): Flux<BankAccount>

    fun countByUserId(userId: UUID): Mono<Long>

    fun save(bankAccount: BankAccount): Mono<BankAccount>

    fun findOneByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<BankAccount?>

    fun deleteByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<Long>

    fun update(
        id: UUID,
        userId: UUID,
        newName: String,
        newEnabled: Boolean,
    ): Mono<Long>
}
