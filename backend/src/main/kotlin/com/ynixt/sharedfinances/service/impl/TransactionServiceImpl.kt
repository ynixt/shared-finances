package com.ynixt.sharedfinances.service.impl

import com.ynixt.sharedfinances.entity.*
import com.ynixt.sharedfinances.mapper.TransactionMapper
import com.ynixt.sharedfinances.model.dto.transaction.*
import com.ynixt.sharedfinances.repository.TransactionRepository
import com.ynixt.sharedfinances.service.CreditCardBillService
import com.ynixt.sharedfinances.service.TransactionService
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class TransactionServiceImpl(
    private val entityManager: EntityManager,
    private val transactionRepository: TransactionRepository,
    private val creditCardBillService: CreditCardBillService,
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val transactionMapper: TransactionMapper
) : TransactionService {
    override fun findAllByIdIncludeGroupAndCategoryAsTransactionDto(
        user: User,
        groupId: Long?,
        bankAccountId: Long?,
        creditCardId: Long?,
        minDate: LocalDate?,
        maxDate: LocalDate?,
        pageable: Pageable
    ): Page<TransactionDto> {
        val page = transactionRepository.findAllByIdIncludeGroupAndCategory(
            userId = if (groupId == null) user.id!! else null,
            groupId = groupId,
            bankAccountId = bankAccountId,
            creditCardId = creditCardId,
            minDate = minDate,
            maxDate = maxDate,
            pageable = pageable
        )

        return page.map { transactionMapper.toDto(it) }
    }

    override fun newTransaction(user: User, newDto: NewTransactionDto): Transaction {
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

        if (newDto is NewBankTransactionDto) {
            val bank = entityManager.getReference(BankAccount::class.java, newDto.bankAccountId)

            transaction.bankAccount = bank
        }

        if (newDto is NewTransferTransactionDto) {
            val user2 = entityManager.getReference(User::class.java, newDto.secondUserId)
            val bank2 = entityManager.getReference(BankAccount::class.java, newDto.bankAccount2Id)

            transaction.secondUser = user2
            transaction.secondBankAccount = bank2
        }

        if (newDto is NewCreditCardTransactionDto) {
            val creditCard = entityManager.getReference(CreditCard::class.java, newDto.creditCardId)
            val creditCardBillDate = creditCardBillService.getOrCreate(newDto.creditCardBillDateValue, creditCard)

            transaction.creditCard = creditCard
            transaction.creditCardBillDate = creditCardBillDate
            transaction.totalInstallments = newDto.totalInstallments
        }

        if (newDto is NewCreditCardBillPaymentTransactionDto) {
            TODO()
        }

        transactionRepository.save(transaction)
        transaction = transactionRepository.findOneByIdIncludeGroupAndCategory(transaction.id!!)!!

        if (newDto is NewBankTransactionDto && group == null) {
            bankAccountTransactionCreated(user, transaction)
        }

        return transaction
    }

    private fun bankAccountTransactionCreated(user: User, transaction: Transaction) {
        val bankAccountId = transaction.bankAccountId!!
        simpMessagingTemplate.convertAndSendToUser(
            user.email,
            "/queue/bank-account/transaction-created/${bankAccountId}",
            transactionMapper.toDto(transaction)!!
        )
        simpMessagingTemplate.convertAndSendToUser(
            user.email,
            "/queue/bank-account/transaction-created",
            transactionMapper.toDto(transaction)!!
        )
    }

    private fun bankAccountTransactionUpdated(user: User, transaction: Transaction) {
        val bankAccountId = transaction.bankAccountId!!
        simpMessagingTemplate.convertAndSendToUser(
            user.email,
            "/queue/bank-account/transaction-updated/${bankAccountId}",
            transactionMapper.toDto(transaction)!!
        )
        simpMessagingTemplate.convertAndSendToUser(
            user.email,
            "/queue/bank-account/transaction-updated",
            transactionMapper.toDto(transaction)!!
        )
    }

    private fun bankAccountTransactionDeleted(user: User, transaction: Transaction) {
        val bankAccountId = transaction.bankAccountId!!
        simpMessagingTemplate.convertAndSendToUser(
            user.email,
            "/queue/bank-account/transaction-deleted/${bankAccountId}",
            transactionMapper.toDto(transaction)!!
        )
        simpMessagingTemplate.convertAndSendToUser(
            user.email,
            "/queue/bank-account/transaction-deleted",
            transactionMapper.toDto(transaction)!!
        )
    }
}
