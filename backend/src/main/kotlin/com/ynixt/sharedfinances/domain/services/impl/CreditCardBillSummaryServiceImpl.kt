package com.ynixt.sharedfinances.domain.services.impl

import com.ynixt.sharedfinances.domain.models.creditcard.CreditCardBill
import com.ynixt.sharedfinances.domain.services.CreditCardBillService
import com.ynixt.sharedfinances.domain.services.CreditCardBillSummaryService
import com.ynixt.sharedfinances.domain.services.walletentry.EntryRecurrenceConfigService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class CreditCardBillSummaryServiceImpl(
    private val creditCardBillService: CreditCardBillService,
    private val entryRecurrenceConfigService: EntryRecurrenceConfigService,
) : CreditCardBillSummaryService {
    override suspend fun getBillForMonth(
        userId: UUID,
        creditCardId: UUID,
        month: Int,
        year: Int,
    ): CreditCardBill {
        val billDate = LocalDate.of(year, month, 1)

        val bill =
            creditCardBillService.getBillFromDatabaseOrSimulate(
                userId = userId,
                creditCardId = creditCardId,
                billDate = billDate,
            )

        val previousBill =
            creditCardBillService.getBillFromDatabaseOrSimulate(
                userId = userId,
                creditCardId = creditCardId,
                billDate = billDate.minusMonths(1),
            )

        bill.startDate = previousBill.closingDate.plusDays(1)

        val valuesFromFuture =
            entryRecurrenceConfigService.getFutureValuesOCreditCard(
                bill = bill,
                walletItemId = creditCardId,
                userId = userId,
                groupId = null,
            )

        return bill.copy(value = bill.value + valuesFromFuture)
    }
}
