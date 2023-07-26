package com.ynixt.sharedfinances.model.dto.transaction

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.ynixt.sharedfinances.enums.TransactionType
import java.math.BigDecimal
import java.time.LocalDate

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(value = RevenueTransactionDto::class, name = "Revenue"),
    JsonSubTypes.Type(value = ExpenseTransactionDto::class, name = "Expense"),
    JsonSubTypes.Type(value = TransferTransactionDto::class, name = "Transfer"),
    JsonSubTypes.Type(value = CreditCardTransactionDto::class, name = "CreditCard"),
    JsonSubTypes.Type(value = CreditCardBillPaymentTransactionDto::class, name = "CreditCardBillPayment"),
)
abstract class TransactionDto(
    val type: TransactionType,
    val date: LocalDate,
    val value: BigDecimal,
    val description: String?,
    val categoryId: Long?,
    val firstUserId: Long,
    val groupId: Long?,
)

abstract class BankTransactionDto(
    val bankAccountId: Long,
    firstUserId: Long,
    groupId: Long?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    categoryId: Long?,
) : TransactionDto(
    firstUserId = firstUserId,
    groupId = groupId,
    type = type,
    date = date,
    value = value,
    description = description,
    categoryId = categoryId,
)

class RevenueTransactionDto(
    bankAccountId: Long,
    firstUserId: Long,
    groupId: Long?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    categoryId: Long?,
) : BankTransactionDto(
    bankAccountId = bankAccountId,
    firstUserId = firstUserId,
    groupId = groupId,
    type = type,
    date = date,
    value = value,
    description = description,
    categoryId = categoryId,
)

class ExpenseTransactionDto(
    bankAccountId: Long,
    firstUserId: Long,
    groupId: Long?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    categoryId: Long?,
) : BankTransactionDto(
    bankAccountId = bankAccountId,
    firstUserId = firstUserId,
    groupId = groupId,
    type = type,
    date = date,
    value = value,
    description = description,
    categoryId = categoryId,
)

class TransferTransactionDto(
    bankAccountId: Long,
    val bankAccount2Id: Long,
    firstUserId: Long,
    groupId: Long?,
    val secondUserId: Long?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    categoryId: Long?,
) : BankTransactionDto(
    bankAccountId = bankAccountId,
    firstUserId = firstUserId,
    groupId = groupId,
    type = type,
    date = date,
    value = value,
    description = description,
    categoryId = categoryId,
)

class CreditCardTransactionDto(
    val creditCardId: Long,
    val creditCardBillId: Long,
    val creditCardBillDateValue: LocalDate?,
    val totalInstallments: Int?,
    firstUserId: Long,
    groupId: Long?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    categoryId: Long?,
) : TransactionDto(
    firstUserId = firstUserId,
    groupId = groupId,
    type = type,
    date = date,
    value = value,
    description = description,
    categoryId = categoryId,
)

class CreditCardBillPaymentTransactionDto(
    val bankAccountId: Long,
    val creditCardId: Long,
    val creditCardBillId: Long,
    val totalInstallments: Int?,
    firstUserId: Long,
    groupId: Long?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    categoryId: Long?,
) : TransactionDto(
    firstUserId = firstUserId,
    groupId = groupId,
    type = type,
    date = date,
    value = value,
    description = description,
    categoryId = categoryId,
)
