package com.ynixt.sharedfinances.model.dto.transaction

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.ynixt.sharedfinances.enums.TransactionType
import com.ynixt.sharedfinances.model.dto.bankAccount.BankAccountNameDto
import com.ynixt.sharedfinances.model.dto.creditcard.CreditCardNameDto
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
    val user: UserDto,
    val group: GroupDto?,
)

abstract class BankTransactionDto(
    id: Long?,
    val bankAccount: BankAccountNameDto,
    userId: Long,
    user: UserDto,
    group: GroupDto?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    category: TransactionCategoryDto?,
) : TransactionDto(
    id = id,
    userId = userId,
    user = user,
    group = group,
    type = type,
    date = date,
    value = value,
    description = description,
    category = category,
)

class RevenueTransactionDto(
    id: Long?,
    bankAccount: BankAccountNameDto,
    userId: Long,
    user: UserDto,
    group: GroupDto?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    category: TransactionCategoryDto?,
) : BankTransactionDto(
    id = id,
    bankAccount = bankAccount,
    userId = userId,
    user = user,
    group = group,
    type = type,
    date = date,
    value = value,
    description = description,
    category = category,
)

class ExpenseTransactionDto(
    id: Long?,
    bankAccount: BankAccountNameDto,
    userId: Long,
    user: UserDto,
    group: GroupDto?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    category: TransactionCategoryDto?,
) : BankTransactionDto(
    id = id,
    bankAccount = bankAccount,
    userId = userId,
    user = user,
    group = group,
    type = type,
    date = date,
    value = value,
    description = description,
    category = category,
)

class OtherSideTransactionDto(
    val id: Long?,
    val bankAccount: BankAccountNameDto,
    val userId: Long,
)

class TransferTransactionDto(
    id: Long?,
    bankAccount: BankAccountNameDto,
    userId: Long,
    user: UserDto,
    group: GroupDto?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    category: TransactionCategoryDto?,
    val otherSide: OtherSideTransactionDto?,
) : BankTransactionDto(
    id = id,
    bankAccount = bankAccount,
    userId = userId,
    user = user,
    group = group,
    type = type,
    date = date,
    value = value,
    description = description,
    category = category,
)

interface ICreditCardTransactionDto {
    val id: Long?
    val creditCard: CreditCardNameDto
    val creditCardBillDateId: Long
    val totalInstallments: Int?
    val installment: Int?
    val installmentId: String?
    val userId: Long
    val user: UserDto
    val group: GroupDto?
    val type: TransactionType
    val date: LocalDate
    val value: BigDecimal
    val description: String?
    val category: TransactionCategoryDto?
}

class CreditCardTransactionDto(
    id: Long?,
    override val creditCard: CreditCardNameDto,
    override val creditCardBillDateId: Long,
    override val totalInstallments: Int?,
    override val installment: Int?,
    override val installmentId: String?,
    userId: Long,
    user: UserDto,
    group: GroupDto?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    category: TransactionCategoryDto?,
) : TransactionDto(
    id = id,
    userId = userId,
    user = user,
    group = group,
    type = type,
    date = date,
    value = value,
    description = description,
    category = category,
), ICreditCardTransactionDto

class CreditCardBillPaymentTransactionDto(
    id: Long?,
    bankAccount: BankAccountNameDto,
    override val creditCard: CreditCardNameDto,
    override val creditCardBillDateId: Long,
    override val totalInstallments: Int?,
    override val installment: Int?,
    override val installmentId: String?,
    userId: Long,
    user: UserDto,
    group: GroupDto?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    category: TransactionCategoryDto?,
) : BankTransactionDto(
    id = id,
    userId = userId,
    user = user,
    group = group,
    type = type,
    date = date,
    value = value,
    description = description,
    category = category,
    bankAccount = bankAccount,
), ICreditCardTransactionDto
