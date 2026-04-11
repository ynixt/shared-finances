package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity
import com.ynixt.sharedfinances.domain.exceptions.http.UnauthorizedException
import com.ynixt.sharedfinances.domain.mapper.CreditCardBillMapper
import com.ynixt.sharedfinances.domain.models.creditcard.CreditCardBill
import com.ynixt.sharedfinances.domain.repositories.CreditCardBillRepository
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.CreditCardService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Service
class CreditCardBillServiceImpl(
    private val creditCardBillRepository: CreditCardBillRepository,
    private val creditCardBillMapper: CreditCardBillMapper,
    private val creditCardService: CreditCardService,
) : CreditCardBillService {
    override suspend fun getOrCreateBill(
        creditCardId: UUID,
        dueDate: LocalDate,
        closingDate: LocalDate,
        startValue: BigDecimal,
    ): CreditCardBillEntity =
        dueDate.withDayOfMonth(1).let { billDate ->
            creditCardBillRepository
                .findOneByCreditCardIdAndBillDate(
                    creditCardId = creditCardId,
                    billDate = billDate,
                ).awaitSingleOrNull() ?: try {
                creditCardBillRepository
                    .save(
                        CreditCardBillEntity(
                            creditCardId = creditCardId,
                            billDate = billDate,
                            dueDate = dueDate,
                            closingDate = closingDate,
                            paid = false,
                            value = startValue,
                        ),
                    ).awaitSingle()
            } catch (ex: Exception) {
                creditCardBillRepository
                    .findOneByCreditCardIdAndBillDate(
                        creditCardId = creditCardId,
                        billDate = billDate,
                    ).awaitSingleOrNull() ?: throw ex
            }
        }

    override suspend fun changeClosingDate(
        userId: UUID,
        creditCardId: UUID,
        closingDate: LocalDate,
    ) {
        creditCardBillRepository.changeClosingDateById(creditCardId, closingDate).awaitSingle()
    }

    override suspend fun changeDueDate(
        userId: UUID,
        creditCardId: UUID,
        dueDate: LocalDate,
    ) {
        creditCardBillRepository.changeDueDateById(creditCardId, dueDate).awaitSingle()
    }

    override suspend fun addValueById(
        id: UUID,
        value: BigDecimal,
    ): Long = creditCardBillRepository.addValueById(id, value).awaitSingle()

    override suspend fun findById(id: UUID): CreditCardBill? =
        creditCardBillRepository.findById(id).awaitSingleOrNull()?.let(creditCardBillMapper::toModel)

    override suspend fun findAuthorizedById(
        userId: UUID,
        id: UUID,
    ): CreditCardBill? =
        creditCardBillRepository
            .findOneByUserIdAndId(
                userId = userId,
                id = id,
            ).awaitSingleOrNull()
            ?.let(creditCardBillMapper::toModel)

    override suspend fun findAllOpenByDueDateBetween(
        userId: UUID,
        minimumDueDate: LocalDate,
        maximumDueDate: LocalDate,
    ): List<CreditCardBill> =
        creditCardBillRepository
            .findAllOpenByUserIdAndDueDateBetween(
                userId = userId,
                minimumDueDate = minimumDueDate,
                maximumDueDate = maximumDueDate,
            ).asFlow()
            .toList()
            .map(creditCardBillMapper::toModel)

    override suspend fun getBillFromDatabaseOrSimulate(
        userId: UUID,
        creditCardId: UUID,
        billDate: LocalDate,
    ): CreditCardBill {
        val creditCard = creditCardService.findOne(userId, creditCardId) ?: throw UnauthorizedException()

        return creditCardBillRepository
            .findOneByUserIdAndCreditCardIdAndBillDate(
                userId = userId,
                creditCardId = creditCardId,
                billDate = billDate,
            ).awaitSingleOrNull()
            .let { databaseBill ->
                if (databaseBill == null) {
                    val dueDate = creditCard.getDueDate(billDate)

                    CreditCardBill(
                        creditCardId = creditCardId,
                        id = null,
                        paid = false,
                        value = BigDecimal.ZERO,
                        dueDate = dueDate,
                        closingDate = creditCard.getClosingDate(dueDate),
                        billDate = billDate,
                    )
                } else {
                    creditCardBillMapper.toModel(databaseBill)
                }
            }
    }
}
