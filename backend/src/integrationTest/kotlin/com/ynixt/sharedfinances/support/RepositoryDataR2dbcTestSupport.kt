package com.ynixt.sharedfinances.support

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.entities.wallet.WalletItemEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.CreditCardBillEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.RecurrenceEventEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEntryEntity
import com.ynixt.sharedfinances.domain.entities.wallet.entries.WalletEventEntity
import com.ynixt.sharedfinances.domain.enums.PaymentType
import com.ynixt.sharedfinances.domain.enums.RecurrenceType
import com.ynixt.sharedfinances.domain.enums.WalletEntryType
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

abstract class RepositoryDataR2dbcTestSupport : IntegrationTestContainers() {
    protected fun newUser(email: String = "user-${UUID.randomUUID()}@example.com"): UserEntity =
        UserEntity(
            email = email,
            passwordHash = "hash",
            firstName = "Test",
            lastName = "User",
            lang = "en",
            defaultCurrency = "BRL",
            tmz = "UTC",
            photoUrl = null,
            emailVerified = true,
            mfaEnabled = false,
            totpSecret = null,
            onboardingDone = true,
        )

    protected fun newBankAccount(
        userId: UUID,
        name: String = "Bank Account",
        balance: BigDecimal = BigDecimal("1000.00"),
    ): WalletItemEntity =
        WalletItemEntity(
            type = WalletItemType.BANK_ACCOUNT,
            name = name,
            enabled = true,
            userId = userId,
            currency = "BRL",
            balance = balance,
            totalLimit = null,
            dueDay = null,
            daysBetweenDueAndClosing = null,
            dueOnNextBusinessDay = null,
        )

    protected fun newCreditCard(
        userId: UUID,
        name: String = "Credit Card",
        limit: BigDecimal = BigDecimal("3000.00"),
    ): WalletItemEntity =
        WalletItemEntity(
            type = WalletItemType.CREDIT_CARD,
            name = name,
            enabled = true,
            userId = userId,
            currency = "BRL",
            balance = limit,
            totalLimit = limit,
            dueDay = 10,
            daysBetweenDueAndClosing = 7,
            dueOnNextBusinessDay = true,
        )

    protected fun newCreditCardBill(
        creditCardId: UUID,
        billDate: LocalDate,
        dueDate: LocalDate = billDate.plusDays(10),
        closingDate: LocalDate = billDate.plusDays(3),
        value: BigDecimal = BigDecimal.ZERO,
    ): CreditCardBillEntity =
        CreditCardBillEntity(
            creditCardId = creditCardId,
            billDate = billDate,
            dueDate = dueDate,
            closingDate = closingDate,
            paid = false,
            value = value,
        )

    protected fun newWalletEvent(
        userId: UUID,
        date: LocalDate,
        name: String,
        paymentType: PaymentType = PaymentType.UNIQUE,
        type: WalletEntryType = WalletEntryType.EXPENSE,
    ): WalletEventEntity =
        WalletEventEntity(
            type = type,
            name = name,
            categoryId = null,
            userId = userId,
            groupId = null,
            tags = listOf("repository-test"),
            observations = null,
            date = date,
            confirmed = true,
            installment = null,
            recurrenceEventId = null,
            paymentType = paymentType,
        )

    protected fun newWalletEntry(
        walletEventId: UUID,
        walletItemId: UUID,
        value: BigDecimal,
        billId: UUID? = null,
    ): WalletEntryEntity =
        WalletEntryEntity(
            value = value,
            walletEventId = walletEventId,
            walletItemId = walletItemId,
            billId = billId,
        )

    protected fun newRecurrenceEvent(
        userId: UUID,
        name: String,
        nextExecution: LocalDate?,
        endExecution: LocalDate?,
        qtyExecuted: Int = 0,
        qtyLimit: Int? = 12,
    ): RecurrenceEventEntity =
        RecurrenceEventEntity(
            name = name,
            categoryId = null,
            userId = userId,
            groupId = null,
            tags = listOf("repository-test"),
            observations = null,
            type = WalletEntryType.EXPENSE,
            periodicity = RecurrenceType.MONTHLY,
            paymentType = PaymentType.RECURRING,
            qtyExecuted = qtyExecuted,
            qtyLimit = qtyLimit,
            lastExecution = null,
            nextExecution = nextExecution,
            endExecution = endExecution,
        )

    protected fun newRecurrenceEntry(
        walletEventId: UUID,
        walletItemId: UUID,
        value: BigDecimal = BigDecimal("-100.00"),
        nextBillDate: LocalDate?,
        lastBillDate: LocalDate?,
    ): RecurrenceEntryEntity =
        RecurrenceEntryEntity(
            value = value,
            walletEventId = walletEventId,
            walletItemId = walletItemId,
            nextBillDate = nextBillDate,
            lastBillDate = lastBillDate,
        )
}
