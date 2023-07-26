package com.ynixt.sharedfinances.mapper

import com.ynixt.sharedfinances.entity.Transaction
import com.ynixt.sharedfinances.enums.TransactionType
import com.ynixt.sharedfinances.model.dto.transaction.*
import org.mapstruct.Mapper
import org.mapstruct.ObjectFactory
import org.mapstruct.ReportingPolicy

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
interface TransactionMapper {
    @ObjectFactory
    fun createTransactionDto(entity: Transaction?): TransactionDto? {
        return if (entity == null) return null else when (entity.type) {
            TransactionType.Revenue -> toRevenueDto(entity)
            TransactionType.Expense -> toExpenseDto(entity)
            TransactionType.CreditCard -> toCreditCardDto(entity)
            TransactionType.Transfer -> toTransferDto(entity)
            TransactionType.CreditCardBillPayment -> toCreditCardBillPaymentDto(entity)
        }
    }

    fun toDto(transaction: Transaction?): TransactionDto?
    fun toRevenueDto(transaction: Transaction?): RevenueTransactionDto?
    fun toExpenseDto(transaction: Transaction?): ExpenseTransactionDto?
    fun toCreditCardDto(transaction: Transaction?): CreditCardTransactionDto?
    fun toTransferDto(transaction: Transaction?): TransferTransactionDto?
    fun toCreditCardBillPaymentDto(transaction: Transaction?): CreditCardBillPaymentTransactionDto?
}
