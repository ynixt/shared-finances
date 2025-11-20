package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity
import com.ynixt.sharedfinances.domain.repositories.CreditCardBillRepository
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Service
class CreditCardBillServiceImpl(
    private val creditCardBillRepository: CreditCardBillRepository,
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
}
