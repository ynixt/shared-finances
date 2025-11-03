package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.domain.entities.wallet.CreditCard
import com.ynixt.sharedfinances.domain.models.creditcard.EditCreditCardRequest
import com.ynixt.sharedfinances.domain.models.creditcard.NewCreditCardRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Mono
import java.util.UUID

interface CreditCardService {
    fun findAll(
        userId: UUID,
        pageable: Pageable,
    ): Mono<Page<CreditCard>>

    fun findOne(
        userId: UUID,
        id: UUID,
    ): Mono<CreditCard>

    fun create(
        userId: UUID,
        request: NewCreditCardRequest,
    ): Mono<CreditCard>

    fun edit(
        userId: UUID,
        id: UUID,
        request: EditCreditCardRequest,
    ): Mono<CreditCard>

    fun delete(
        userId: UUID,
        id: UUID,
    ): Mono<Boolean>
}
