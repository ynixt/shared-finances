package com.ynixt.sharedfinances.domain.services.impl

import com.sun.org.apache.xalan.internal.lib.ExsltDatetime.year
import com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity
import com.ynixt.sharedfinances.domain.mapper.CreditCardBillMapper
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCardBill
import com.ynixt.sharedfinances.domain.repositories.CreditCardBillRepository
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.CreditCardService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Service
class CreditCardBillServiceImpl(
    private val creditCardBillRepository: CreditCardBillRepository,
    private val creditCardBillMapper: CreditCardBillMapper,
    private val creditCardService: CreditCardService,
    cardService: CreditCardService,
) : CreditCardBillService {
    override fun getOrCreateBill(
        creditCardId: UUID,
        dueDate: LocalDate,
        closingDate: LocalDate,
        startValue: BigDecimal,
    ): Mono<CreditCardBillEntity> =
        dueDate.withDayOfMonth(1).let { billDate ->
            creditCardBillRepository
                .findOneByCreditCardIdAndBillDate(
                    creditCardId = creditCardId,
                    billDate = billDate,
                ).switchIfEmpty {
                    creditCardBillRepository
                        .save(
                            CreditCardBillEntity(
                                creditCardId = creditCardId,
                                billDate = billDate,
                                dueDate = dueDate,
                                closingDate = closingDate,
                                payed = false,
                                value = startValue,
                            ),
                        ).onErrorResume { error ->
                            creditCardBillRepository
                                .findOneByCreditCardIdAndBillDate(
                                    creditCardId = creditCardId,
                                    billDate = billDate,
                                ).switchIfEmpty(Mono.error(error))
                        }
                }
        }

    override fun getBillForMonth(
        userId: UUID,
        creditCardId: UUID,
        month: Int,
        year: Int,
    ): Mono<CreditCardBill> {
        val billDate = LocalDate.of(year, month, 1)

        return creditCardBillRepository
            .findOneByUserIdAndCreditCardIdAndBillDate(
                userId = userId,
                creditCardId = creditCardId,
                billDate = billDate,
            ).map(creditCardBillMapper::toModel)
            .switchIfEmpty {
                creditCardService.findOne(userId, creditCardId).map { creditCard ->
                    val dueDate = creditCard.getDueDate(billDate)

                    CreditCardBill(
                        creditCardId = creditCardId,
                        id = null,
                        payed = false,
                        value = BigDecimal.ZERO,
                        dueDate = dueDate,
                        closingDate = creditCard.getClosingDate(dueDate),
                        billDate = billDate,
                    )
                }
            }
    }

    override fun changeClosingDate(
        userId: UUID,
        creditCardId: UUID,
        closingDate: LocalDate,
    ): Mono<Unit> = creditCardBillRepository.changeClosingDateById(creditCardId, closingDate).map {}

    override fun changeDueDate(
        userId: UUID,
        creditCardId: UUID,
        dueDate: LocalDate,
    ): Mono<Unit> = creditCardBillRepository.changeDueDateById(creditCardId, dueDate).map {}

    override fun addValueById(
        id: UUID,
        value: BigDecimal,
    ): Mono<Long> = creditCardBillRepository.addValueById(id, value)
}
