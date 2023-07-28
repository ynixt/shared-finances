package com.ynixt.sharedfinances.model.dto.transaction

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.ynixt.sharedfinances.enums.TransactionType
import com.ynixt.sharedfinances.model.dto.group.GroupDto
import com.ynixt.sharedfinances.model.dto.transactioncategory.TransactionCategoryDto
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
    val category: TransactionCategoryDto?,
    val firstUserId: Long,
    val group: GroupDto?,
)

abstract class BankTransactionDto(
    val bankAccountId: Long,
    firstUserId: Long,
    group: GroupDto?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    category: TransactionCategoryDto?,
) : TransactionDto(
    firstUserId = firstUserId,
    group = group,
    type = type,
    date = date,
    value = value,
    description = description,
    category = category,
)

class RevenueTransactionDto(
    bankAccountId: Long,
    firstUserId: Long,
    group: GroupDto?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    category: TransactionCategoryDto?,
) : BankTransactionDto(
    bankAccountId = bankAccountId,
    firstUserId = firstUserId,
    group = group,
    type = type,
    date = date,
    value = value,
    description = description,
    category = category,
)

class ExpenseTransactionDto(
    bankAccountId: Long,
    firstUserId: Long,
    group: GroupDto?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    category: TransactionCategoryDto?,
) : BankTransactionDto(
    bankAccountId = bankAccountId,
    firstUserId = firstUserId,
    group = group,
    type = type,
    date = date,
    value = value,
    description = description,
    category = category,
)

class TransferTransactionDto(
    bankAccountId: Long,
    val bankAccount2Id: Long,
    firstUserId: Long,
    group: GroupDto?,
    val secondUserId: Long?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    category: TransactionCategoryDto?,
) : BankTransactionDto(
    bankAccountId = bankAccountId,
    firstUserId = firstUserId,
    group = group,
    type = type,
    date = date,
    value = value,
    description = description,
    category = category,
)

class CreditCardTransactionDto(
    val creditCardId: Long,
    val creditCardBillId: Long,
    val totalInstallments: Int?,
    firstUserId: Long,
    group: GroupDto?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    category: TransactionCategoryDto?,
) : TransactionDto(
    firstUserId = firstUserId,
    group = group,
    type = type,
    date = date,
    value = value,
    description = description,
    category = category,
)

class CreditCardBillPaymentTransactionDto(
    val bankAccountId: Long,
    val creditCardId: Long,
    val creditCardBillId: Long,
    val totalInstallments: Int?,
    firstUserId: Long,
    group: GroupDto?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    category: TransactionCategoryDto?,
) : TransactionDto(
    firstUserId = firstUserId,
    group = group,
    type = type,
    date = date,
    value = value,
    description = description,
    category = category,
)
