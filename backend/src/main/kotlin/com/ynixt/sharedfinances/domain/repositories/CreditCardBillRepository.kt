package com.ynixt.sharedfinances.domain.repositories

import com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

interface CreditCardBillRepository {
    fun findOneByCreditCardIdAndBillDate(
        creditCardId: UUID,
        billDate: LocalDate,
    ): Mono<CreditCardBillEntity>

    fun findOneByUserIdAndCreditCardIdAndBillDate(
        userId: UUID,
        creditCardId: UUID,
        billDate: LocalDate,
    ): Mono<CreditCardBillEntity>

    fun save(creditCardBillEntity: CreditCardBillEntity): Mono<CreditCardBillEntity>

    fun addValueById(
        id: UUID,
        value: BigDecimal,
    ): Mono<Long>

    fun changeDueDateById(
        id: UUID,
        dueDate: LocalDate,
    ): Mono<Long>

    fun changeClosingDateById(
        id: UUID,
        closingDate: LocalDate,
    ): Mono<Long>
}
