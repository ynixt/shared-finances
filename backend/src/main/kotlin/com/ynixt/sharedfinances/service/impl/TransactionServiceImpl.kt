package com.ynixt.sharedfinances.service.impl

import com.ynixt.sharedfinances.entity.*
import com.ynixt.sharedfinances.model.dto.transaction.*
import com.ynixt.sharedfinances.repository.TransactionRepository
import com.ynixt.sharedfinances.service.CreditCardBillService
import com.ynixt.sharedfinances.service.TransactionService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service

@Service
class TransactionServiceImpl(
    private val entityManager: EntityManager,
    private val transactionRepository: TransactionRepository,
    private val creditCardBillService: CreditCardBillService
) : TransactionService {
    override fun newTransaction(user: User, newDto: TransactionDto): Transaction {
        val firstUser = entityManager.getReference(User::class.java, newDto.firstUserId)
        val group = if (newDto.groupId == null) null else entityManager.getReference(Group::class.java, newDto.groupId)
        val category = if (newDto.categoryId == null) null else entityManager.getReference(
            TransactionCategory::class.java, newDto.categoryId
        )

        var transaction = Transaction(
            type = newDto.type,
            category = category,
            group = group,
            user = firstUser,
            date = newDto.date,
            value = newDto.value,
            description = newDto.description
        )

        if (newDto is BankTransactionDto) {
            val bank = entityManager.getReference(BankAccount::class.java, newDto.bankAccountId)

            transaction.bankAccount = bank
        }

        if (newDto is TransferTransactionDto) {
            val user2 = entityManager.getReference(User::class.java, newDto.secondUserId)
            val bank2 = entityManager.getReference(BankAccount::class.java, newDto.bankAccount2Id)

            transaction.secondUser = user2
            transaction.secondBankAccount = bank2
        }

        if (newDto is CreditCardTransactionDto) {
            val creditCard = entityManager.getReference(CreditCard::class.java, newDto.creditCardId)
            val creditCardBillDate = creditCardBillService.getOrCreate(newDto.creditCardBillDateValue!!, creditCard)

            transaction.creditCard = creditCard
            transaction.creditCardBillDate = creditCardBillDate
            transaction.totalInstallments = newDto.totalInstallments
        }

        if (newDto is CreditCardBillPaymentTransactionDto) {
            TODO()
        }

        transaction = transactionRepository.save(transaction)

        return transaction
    }
}
