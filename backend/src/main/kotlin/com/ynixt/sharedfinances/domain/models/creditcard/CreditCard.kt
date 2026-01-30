package com.ynixt.sharedfinances.domain.models.creditcard

import com.ynixt.sharedfinances.domain.enums.WalletItemType
import com.ynixt.sharedfinances.domain.models.WalletItem
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

class CreditCard(
    name: String,
    enabled: Boolean,
    userId: UUID,
    currency: String,
    val totalLimit: BigDecimal,
    /**
     * Also known as availableLimit
     **/
    balance: BigDecimal,
    val dueDay: Int,
    val daysBetweenDueAndClosing: Int,
    val dueOnNextBusinessDay: Boolean = true,
) : WalletItem(
        name = name,
        enabled = enabled,
        userId = userId,
        currency = currency,
        balance = balance,
    ) {
    override val type: WalletItemType = WalletItemType.CREDIT_CARD

    fun getDueDate(billDate: LocalDate): LocalDate {
        var dueDate = billDate.withDayOfMonth(dueDay)

        if (dueOnNextBusinessDay && dueDate.dayOfWeek.value > 5) {
            if (dueDate.dayOfWeek.value == 6) {
                dueDate = dueDate.plusDays(2)
            } else {
                dueDate = dueDate.plusDays(1)
            }
        }

        return dueDate
    }

    fun getClosingDate(dueDate: LocalDate): LocalDate = dueDate.minusDays(daysBetweenDueAndClosing.toLong())

    fun getBestBill(transactionDate: LocalDate): LocalDate {
        var date = transactionDate.withDayOfMonth(1).withDayOfMonth(dueDay.coerceIn(1, transactionDate.lengthOfMonth()))

        if (dueOnNextBusinessDay) {
            date = skipWeekend(date)
        }

        date = date.minusDays(daysBetweenDueAndClosing.toLong())

        if (transactionDate.isAfter(date)) {
            date = date.withDayOfMonth(1).plusMonths(1)
        }

        return date.withDayOfMonth(1)
    }

    private fun skipWeekend(date: LocalDate): LocalDate =
        when (date.dayOfWeek) {
            DayOfWeek.SATURDAY -> date.plusDays(2)
            DayOfWeek.SUNDAY -> date.plusDays(1)
            else -> date
        }
}
