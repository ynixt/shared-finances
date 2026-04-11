package com.ynixt.sharedfinances.resources.services

import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.exceptions.http.InvalidCreditCardBillPaymentAmountException
import com.ynixt.sharedfinances.domain.exceptions.http.OriginNotFoundException
import com.ynixt.sharedfinances.domain.exceptions.http.UnauthorizedException
import com.ynixt.sharedfinances.domain.models.walletentry.NewEntryRequest
import com.ynixt.sharedfinances.domain.services.BankAccountService
import com.ynixt.sharedfinances.domain.services.CreditCardBillPaymentService
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.walletentry.WalletEntryCreateService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Service
class CreditCardBillPaymentServiceImpl(
    private val creditCardBillService: CreditCardBillService,
    private val bankAccountService: BankAccountService,
    private val walletEntryCreateService: WalletEntryCreateService,
) : CreditCardBillPaymentService {
    @Transactional
    override suspend fun payBill(
        userId: UUID,
        billId: UUID,
        bankAccountId: UUID,
        date: LocalDate,
        amount: BigDecimal,
        observations: String?,
    ) {
        val bill = creditCardBillService.findAuthorizedById(userId = userId, id = billId) ?: throw UnauthorizedException()
        val bankAccount =
            bankAccountService.findBankAccount(userId = userId, id = bankAccountId)
                ?: throw OriginNotFoundException(bankAccountId)

        val remaining = bill.value.negate().max(BigDecimal.ZERO)
        if (amount <= BigDecimal.ZERO || amount > remaining || remaining <= BigDecimal.ZERO) {
            throw InvalidCreditCardBillPaymentAmountException(amount = amount, remaining = remaining)
        }

        walletEntryCreateService.create(
            userId = userId,
            newEntryRequest =
                NewEntryRequest(
                    type = WalletEntryType.TRANSFER,
                    originId = bankAccount.id!!,
                    targetId = bill.creditCardId,
                    name = "Pagamento de fatura",
                    date = date,
                    originValue = amount,
                    targetValue = amount,
                    confirmed = true,
                    observations = observations,
                    paymentType = PaymentType.UNIQUE,
                    targetBillDate = bill.billDate,
                ),
        ) ?: throw UnauthorizedException()
    }
}
