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
    JsonSubTypes.Type(value = NewRevenueTransactionDto::class, name = "Revenue"),
    JsonSubTypes.Type(value = NewExpenseTransactionDto::class, name = "Expense"),
    JsonSubTypes.Type(value = NewTransferTransactionDto::class, name = "Transfer"),
    JsonSubTypes.Type(value = NewCreditCardTransactionDto::class, name = "CreditCard"),
    JsonSubTypes.Type(value = NewCreditCardBillPaymentTransactionDto::class, name = "CreditCardBillPayment"),
)
abstract class NewTransactionDto(
    val type: TransactionType,
    val date: LocalDate,
    val value: BigDecimal,
    val description: String?,
    val categoriesIds: List<Long>?,
    val firstUserId: Long?,
    val groupId: Long?,
)

abstract class NewBankTransactionDto(
    val bankAccountId: Long,
    firstUserId: Long?,
    groupId: Long?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    categoriesIds: List<Long>?,
) : NewTransactionDto(
    firstUserId = firstUserId,
    groupId = groupId,
    type = type,
    date = date,
    value = value,
    description = description,
    categoriesIds = categoriesIds,
)

class NewRevenueTransactionDto(
    bankAccountId: Long,
    firstUserId: Long?,
    groupId: Long?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    categoriesIds: List<Long>?,
) : NewBankTransactionDto(
    bankAccountId = bankAccountId,
    firstUserId = firstUserId,
    groupId = groupId,
    type = type,
    date = date,
    value = value,
    description = description,
    categoriesIds = categoriesIds,
)

class NewExpenseTransactionDto(
    bankAccountId: Long,
    firstUserId: Long?,
    groupId: Long?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    categoriesIds: List<Long>?,
) : NewBankTransactionDto(
    bankAccountId = bankAccountId,
    firstUserId = firstUserId,
    groupId = groupId,
    type = type,
    date = date,
    value = value,
    description = description,
    categoriesIds = categoriesIds,
)

class NewTransferTransactionDto(
    bankAccountId: Long,
    val bankAccount2Id: Long,
    firstUserId: Long?,
    groupId: Long?,
    val secondUserId: Long?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    categoriesIds: List<Long>?,
) : NewBankTransactionDto(
    bankAccountId = bankAccountId,
    firstUserId = firstUserId,
    groupId = groupId,
    type = type,
    date = date,
    value = value,
    description = description,
    categoriesIds = categoriesIds,
)

interface INewCreditCardTransactionDto {
    val creditCardId: Long
    val creditCardBillDateValue: LocalDate
    val totalInstallments: Int?
    val firstUserId: Long?
    val groupId: Long?
    val type: TransactionType
    val date: LocalDate
    val value: BigDecimal
    val description: String?
    val categoriesIds: List<Long>?
}

class NewCreditCardTransactionDto(
    override val creditCardId: Long,
    override val creditCardBillDateValue: LocalDate,
    override val totalInstallments: Int?,
    val creditReversal: Boolean,
    firstUserId: Long?,
    groupId: Long?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    categoriesIds: List<Long>?,
) : NewTransactionDto(
    firstUserId = firstUserId,
    groupId = groupId,
    type = type,
    date = date,
    value = value,
    description = description,
    categoriesIds = categoriesIds,
), INewCreditCardTransactionDto

class NewCreditCardBillPaymentTransactionDto(
    bankAccountId: Long,
    override val creditCardId: Long,
    override val creditCardBillDateValue: LocalDate,
    override val totalInstallments: Int?,
    firstUserId: Long?,
    groupId: Long?,
    type: TransactionType,
    date: LocalDate,
    value: BigDecimal,
    description: String?,
    categoriesIds: List<Long>?,
) : NewBankTransactionDto(
    bankAccountId = bankAccountId,
    firstUserId = firstUserId,
    groupId = groupId,
    type = type,
    date = date,
    value = value,
    description = description,
    categoriesIds = categoriesIds,
), INewCreditCardTransactionDto
