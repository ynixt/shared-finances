package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.CreditCard
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface CreditCardRepository {
    fun findAllByUserId(
        userId: UUID,
        pageable: Pageable,
    ): Flux<CreditCard>

    fun countByUserId(userId: UUID): Mono<Long>

    fun save(creditCard: CreditCard): Mono<CreditCard>

    fun findOneByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<CreditCard>

    fun deleteByIdAndUserId(
        id: UUID,
        userId: UUID,
    ): Mono<Long>

    fun update(
        id: UUID,
        userId: UUID,
        newName: String,
        newEnabled: Boolean,
        newCurrency: String,
        newTotalLimit: java.math.BigDecimal,
        newDueDay: Int,
        newDaysBetweenDueAndClosing: Int,
        newDueOnNextBusinessDay: Boolean,
    ): Mono<Long>
}
