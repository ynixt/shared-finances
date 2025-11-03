package com.ynixt.sharedfinances.domain.services.actionevents

import com.ynixt.sharedfinances.domain.entities.wallet.CreditCard
import reactor.core.publisher.Mono
import java.util.UUID

interface CreditCardActionEventService {
    fun sendInsertedCreditCard(
        userId: UUID,
        creditCard: CreditCard,
    ): Mono<Long>

    fun sendUpdatedCreditCard(
        userId: UUID,
        creditCard: CreditCard,
    ): Mono<Long>

    fun sendDeletedCreditCard(
        userId: UUID,
        id: UUID,
    ): Mono<Long>
}
