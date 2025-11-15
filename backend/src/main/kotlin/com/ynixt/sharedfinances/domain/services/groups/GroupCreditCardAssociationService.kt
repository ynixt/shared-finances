package com.ynixt.sharedfinances.domain.services.groups

import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupCreditCardAssociationService {
    fun findAllAllowedCreditCardsToAssociate(
        userId: UUID,
        groupId: UUID,
    ): Mono<List<CreditCard>>

    fun findAllAssociatedCreditCards(
        userId: UUID,
        groupId: UUID,
    ): Mono<List<CreditCard>>

    fun associateCreditCard(
        userId: UUID,
        groupId: UUID,
        creditCardId: UUID,
    ): Mono<Unit>

    fun unassociateCreditCard(
        userId: UUID,
        groupId: UUID,
        creditCardId: UUID,
    ): Mono<Unit>
}
