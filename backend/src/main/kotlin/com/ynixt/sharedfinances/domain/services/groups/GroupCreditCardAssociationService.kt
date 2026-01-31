package com.ynixt.sharedfinances.domain.services.groups

import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import java.util.UUID

interface GroupCreditCardAssociationService {
    suspend fun findAllAllowedCreditCardsToAssociate(
        userId: UUID,
        groupId: UUID,
    ): List<CreditCard>

    suspend fun findAllAssociatedCreditCards(
        userId: UUID,
        groupId: UUID,
    ): List<CreditCard>

    suspend fun associateCreditCard(
        userId: UUID,
        groupId: UUID,
        creditCardId: UUID,
    ): Boolean

    suspend fun unassociateCreditCard(
        userId: UUID,
        groupId: UUID,
        creditCardId: UUID,
    ): Boolean
}
