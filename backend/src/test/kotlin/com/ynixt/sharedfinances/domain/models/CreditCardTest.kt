package com.ynixt.sharedfinances.domain.models

import com.ynixt.sharedfinances.domain.models.creditcard.CreditCard
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class CreditCardTest {
    @Test
    fun `The bill of the current month is already overdue - Should use next month`() {
        val creditCard =
            CreditCard(
                name = "test",
                enabled = true,
                userId = UUID.randomUUID(),
                currency = "BRL",
                totalLimit = BigDecimal.TEN,
                balance = BigDecimal.TEN,
                dueDay = 1,
                daysBetweenDueAndClosing = 10,
                dueOnNextBusinessDay = true,
            )

        val transactionDate = LocalDate.of(2026, 2, 1)

        val expected = LocalDate.of(2026, 3, 1)
        val result = creditCard.getBestBill(transactionDate)

        assertEquals(expected, result)
    }

    @Test
    fun `The bill of the current month is open - Should use it`() {
        val creditCard =
            CreditCard(
                name = "test",
                enabled = true,
                userId = UUID.randomUUID(),
                currency = "BRL",
                totalLimit = BigDecimal.TEN,
                balance = BigDecimal.TEN,
                dueDay = 10,
                daysBetweenDueAndClosing = 6,
                dueOnNextBusinessDay = true,
            )

        val transactionDate = LocalDate.of(2026, 2, 2)

        val expected = LocalDate.of(2026, 2, 1)
        val result = creditCard.getBestBill(transactionDate)

        assertEquals(expected, result)
    }

    @Test
    fun `The bill of the current month is closed -  Should use next month`() {
        val creditCard =
            CreditCard(
                name = "test",
                enabled = true,
                userId = UUID.randomUUID(),
                currency = "BRL",
                totalLimit = BigDecimal.TEN,
                balance = BigDecimal.TEN,
                dueDay = 10,
                daysBetweenDueAndClosing = 7,
                dueOnNextBusinessDay = true,
            )

        val transactionDate = LocalDate.of(2026, 2, 4)

        val expected = LocalDate.of(2026, 3, 1)
        val result = creditCard.getBestBill(transactionDate)

        assertEquals(expected, result)
    }
}
