package com.ynixt.sharedfinances.domain.services.actionevents

import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import java.util.UUID

interface CreditCardActionEventService {
    suspend fun sendInsertedCreditCard(
        userId: UUID,
        creditCard: CreditCard,
    )

    suspend fun sendUpdatedCreditCard(
        userId: UUID,
        creditCard: CreditCard,
    )

    suspend fun sendDeletedCreditCard(
        userId: UUID,
        id: UUID,
    )
}
