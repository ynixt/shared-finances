package com.ynixt.sharedfinances.model.dto.transaction

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.ynixt.sharedfinances.enums.TransactionType
import com.ynixt.sharedfinances.model.dto.bankAccount.BankAccountNameDto
import com.ynixt.sharedfinances.model.dto.group.GroupDto
import com.ynixt.sharedfinances.model.dto.transactioncategory.TransactionCategoryDto
import com.ynixt.sharedfinances.model.dto.user.UserDto
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
    val id: Long?,
    val type: TransactionType,
    val date: LocalDate,
    val value: BigDecimal,
    val description: String?,
    val category: TransactionCategoryDto?,
    val userId: Long,
    val group: GroupDto?,
)

abstract class BankTransactionDto(
    id: Long?,
    val bankAccountId: Long,
    userId: Long,
    group: GroupDto?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    category: TransactionCategoryDto?,
) : TransactionDto(
    id = id,
    userId = userId,
    group = group,
    type = type,
    date = date,
    value = value,
    description = description,
    category = category,
)

class RevenueTransactionDto(
    id: Long?,
    bankAccountId: Long,
    userId: Long,
    group: GroupDto?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    category: TransactionCategoryDto?,
) : BankTransactionDto(
    id = id,
    bankAccountId = bankAccountId,
    userId = userId,
    group = group,
    type = type,
    date = date,
    value = value,
    description = description,
    category = category,
)

class ExpenseTransactionDto(
    id: Long?,
    bankAccountId: Long,
    userId: Long,
    group: GroupDto?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    category: TransactionCategoryDto?,
) : BankTransactionDto(
    id = id,
    bankAccountId = bankAccountId,
    userId = userId,
    group = group,
    type = type,
    date = date,
    value = value,
    description = description,
    category = category,
)

class OtherSideTransactionDto(
    val id: Long?,
    val bankAccountId: Long,
    val bankAccount: BankAccountNameDto,
    val userId: Long,
    val user: UserDto,
)

class TransferTransactionDto(
    id: Long?,
    bankAccountId: Long,
    userId: Long,
    group: GroupDto?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    category: TransactionCategoryDto?,
    val otherSide: OtherSideTransactionDto?,
) : BankTransactionDto(
    id = id,
    bankAccountId = bankAccountId,
    userId = userId,
    group = group,
    type = type,
    date = date,
    value = value,
    description = description,
    category = category,
)

class CreditCardTransactionDto(
    id: Long?,
    val creditCardId: Long,
    val creditCardBillId: Long,
    val totalInstallments: Int?,
    userId: Long,
    group: GroupDto?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    category: TransactionCategoryDto?,
) : TransactionDto(
    id = id,
    userId = userId,
    group = group,
    type = type,
    date = date,
    value = value,
    description = description,
    category = category,
)

class CreditCardBillPaymentTransactionDto(
    id: Long?,
    val bankAccountId: Long,
    val creditCardId: Long,
    val creditCardBillId: Long,
    val totalInstallments: Int?,
    userId: Long,
    group: GroupDto?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    category: TransactionCategoryDto?,
) : TransactionDto(
    id = id,
    userId = userId,
    group = group,
    type = type,
    date = date,
    value = value,
    description = description,
    category = category,
)
