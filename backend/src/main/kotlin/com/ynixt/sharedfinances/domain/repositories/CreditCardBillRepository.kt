package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.UUID

interface CreditCardBillRepository {
    fun findOneByCreditCardIdAndBillDate(
        creditCardId: UUID,
        billDate: LocalDate,
    ): Mono<CreditCardBillEntity>

    fun save(creditCardBillEntity: CreditCardBillEntity): Mono<CreditCardBillEntity>
}
