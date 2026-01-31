package com.ynixt.sharedfinances.domain.services

import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import com.ynixt.sharedfinances.domain.models.creditcard.EditCreditCardRequest
import com.ynixt.sharedfinances.domain.models.creditcard.NewCreditCardRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface CreditCardService {
    suspend fun findAll(
        userId: UUID,
        pageable: Pageable,
    ): Page<CreditCard>

    suspend fun findOne(
        userId: UUID,
        id: UUID,
    ): CreditCard?

    suspend fun create(
        userId: UUID,
        request: NewCreditCardRequest,
    ): CreditCard

    suspend fun edit(
        userId: UUID,
        id: UUID,
        request: EditCreditCardRequest,
    ): CreditCard?

    suspend fun delete(
        userId: UUID,
        id: UUID,
    ): Boolean
}
