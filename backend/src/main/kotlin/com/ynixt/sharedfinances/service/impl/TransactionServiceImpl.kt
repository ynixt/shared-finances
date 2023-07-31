package com.ynixt.sharedfinances.service.impl

import com.ynixt.sharedfinances.entity.*
import com.ynixt.sharedfinances.enums.TransactionType
import com.ynixt.sharedfinances.mapper.TransactionMapper
import com.ynixt.sharedfinances.model.dto.transaction.*
import com.ynixt.sharedfinances.model.exceptions.SFException
import com.ynixt.sharedfinances.model.exceptions.SFExceptionForbidden
import com.ynixt.sharedfinances.repository.TransactionRepository
import com.ynixt.sharedfinances.repository.UserRepository
import com.ynixt.sharedfinances.service.CreditCardBillService
import com.ynixt.sharedfinances.service.CreditCardService
import com.ynixt.sharedfinances.service.GroupService
import com.ynixt.sharedfinances.service.TransactionService
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
class TransactionServiceImpl(
    private val entityManager: EntityManager,
    private val transactionRepository: TransactionRepository,
    private val creditCardBillService: CreditCardBillService,
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val transactionMapper: TransactionMapper,
    private val groupService: GroupService,
    private val creditCardService: CreditCardService,
    private val userRepository: UserRepository
) : TransactionService {
    override fun findOneIncludeGroupAndCategory(
        id: Long, user: User, groupId: Long?
    ): Transaction? {
        if (groupId != null && !groupService.userHasPermissionToGroup(user, groupId)) {
            throw SFExceptionForbidden()
        }

        return transactionRepository.findOneIncludeGroupAndCategory(
            id = id, userId = if (groupId == null) user.id!! else null, groupId = groupId
        )
    }

    override fun findAllIncludeGroupAndCategoryAsTransactionDto(
        user: User,
        groupId: Long?,
        bankAccountId: Long?,
        creditCardId: Long?,
        minDate: LocalDate?,
        maxDate: LocalDate?,
        creditCardBillDate: LocalDate?,
        pageable: Pageable
    ): Page<TransactionDto> {
        if (groupId != null && !groupService.userHasPermissionToGroup(user, groupId)) {
            throw SFExceptionForbidden()
        }

        val page = transactionRepository.findAllIncludeGroupAndCategory(
            userId = if (groupId == null) user.id!! else null,
            groupId = groupId,
            bankAccountId = bankAccountId,
            creditCardId = creditCardId,
            minDate = minDate,
            maxDate = maxDate,
            creditCardBillDate = creditCardBillDate,
            pageable = pageable
        )

        return page.map { transactionMapper.toDto(it) }
    }

    @Transactional()
    override fun newTransaction(user: User, newDto: NewTransactionDto): Transaction {
        val targetUser =
            if (newDto.firstUserId == null || newDto.firstUserId == user.id) user else userRepository.findByIdOrNull(
                newDto.firstUserId
            )!!
        val group = if (newDto.groupId == null) null else entityManager.getReference(Group::class.java, newDto.groupId)
        val category = if (newDto.categoryId == null) null else entityManager.getReference(
            TransactionCategory::class.java, newDto.categoryId
        )
        var otherSideTransaction: Transaction? = null

        if (newDto.groupId != null && !groupService.userHasPermissionToGroup(user, newDto.groupId)) {
            throw SFExceptionForbidden()
        }

        var transaction = Transaction(
            type = newDto.type,
            category = category,
            group = group,
            user = targetUser,
            date = newDto.date,
            value = newDto.value,
            description = newDto.description
        ).apply {
            categoryId = newDto.categoryId
            groupId = newDto.groupId
            userId = newDto.firstUserId
        }

        if (newDto is NewBankTransactionDto) {
            applyBankDtoIntoEntity(transaction, newDto)
        }

        if (newDto is NewTransferTransactionDto) {
            applyTransferDtoIntoEntity(transaction, newDto)
            otherSideTransaction = transaction.otherSide
        }

        if (newDto is INewCreditCardTransactionDto) {
            applyCreditCardDtoIntoEntity(transaction, newDto)

            if (newDto.totalInstallments != null) {
                transaction.installment = 1
                transaction.installmentId = UUID.randomUUID().toString()

                val creditCard = transaction.creditCard!!

                transactionRepository.saveAll(
                    (2..newDto.totalInstallments!!).map { i ->
                        transaction.copy(
                            id = null,
                            installment = i,
                            date = transaction.date.plusMonths((i - 1).toLong()),
                            creditCardBillDate = creditCardBillService.getOrCreate(
                                creditCardBillService.getNextBillDateValue(
                                    newDto.creditCardBillDateValue,
                                    creditCardClosingDay = creditCard.closingDay,
                                    next = i - 1
                                ), creditCard
                            )
                        )
                    }
                )
            }
        }

        if (newDto is NewCreditCardTransactionDto) {
            creditCardService.addToAvailableLimit(targetUser, newDto.creditCardId, newDto.value)
        }

        if (newDto is NewCreditCardBillPaymentTransactionDto) {
            creditCardService.addToAvailableLimit(targetUser, newDto.creditCardId, newDto.value.negate())
        }

        transactionRepository.saveAndFlush(transaction)

        if (otherSideTransaction != null) {
            otherSideTransaction.otherSide = transaction
            transactionRepository.saveAndFlush(otherSideTransaction)
        }

        entityManager.clear()

        transaction = findOneIncludeGroupAndCategory(
            id = transaction.id!!, user = user, groupId = newDto.groupId
        )!!

        if (newDto is NewBankTransactionDto) {
            bankAccountTransactionCreated(targetUser, transaction)
        }

        if (newDto is INewCreditCardTransactionDto) {
            creditCardTransactionCreated(targetUser, transaction)
        }

        return transaction
    }

    @Transactional
    override fun editTransaction(user: User, id: Long, editDto: NewTransactionDto): Transaction {
        val category = if (editDto.categoryId == null) null else entityManager.getReference(
            TransactionCategory::class.java, editDto.categoryId
        )

        val transaction = (findOne(
            id = id, user = user, groupId = editDto.groupId
        ) ?: throw SFException(
            reason = "Transaction not found"
        ))

        val targetUser =
            if (editDto.firstUserId == null || editDto.firstUserId == user.id) user else userRepository.findByIdOrNull(
                editDto.firstUserId
            )!!

        val oldUserId = transaction.userId
        val oldValue = transaction.value
        val oldCreditCardId = transaction.creditCardId

        transaction.apply {
            type = editDto.type
            this.user = targetUser
            this.userId = editDto.firstUserId
            this.category = category
            categoryId = editDto.categoryId
            date = editDto.date
            value = editDto.value
            description = editDto.description
            bankAccount = null
            bankAccountId = null
            creditCard = null
            creditCardId = null
            creditCardBillDate = null
            totalInstallments = null
        }

        if (editDto is NewBankTransactionDto) {
            applyBankDtoIntoEntity(transaction, editDto)
        }

        if (editDto is NewTransferTransactionDto) {
            TODO()
        }

        if (editDto is NewCreditCardTransactionDto) {
            applyCreditCardDtoIntoEntity(transaction, editDto)
        }

        if (editDto is NewCreditCardBillPaymentTransactionDto) {
            TODO()
        }

        transactionRepository.saveAndFlush(transaction)
        entityManager.clear()

        val updatedTransaction = findOneIncludeGroupAndCategory(
            id = transaction.id!!, user = user, groupId = editDto.groupId
        )!!

        if (editDto is NewBankTransactionDto) {
            bankAccountTransactionUpdated(targetUser, updatedTransaction)
        }

        if (editDto is NewCreditCardTransactionDto) {
            creditCardTransactionUpdated(targetUser, updatedTransaction)
            creditCardService.addToAvailableLimit(targetUser, editDto.creditCardId, editDto.value)
        }

        if (oldCreditCardId != null) {
            creditCardService.addToAvailableLimit(
                if (oldUserId == user.id) user else userRepository.findByIdOrNull(oldUserId!!)!!,
                oldCreditCardId,
                oldValue.negate()
            )
        }

        return updatedTransaction
    }

    @Transactional
    override fun delete(user: User, id: Long, groupId: Long?) {
        val transaction = findOneIncludeGroupAndCategory(
            id = id, user = user, groupId = groupId
        )

        if (transaction != null) {
            transactionRepository.deleteById(transaction.id!!)

            if (transaction.type == TransactionType.Revenue || transaction.type == TransactionType.Expense || transaction.type == TransactionType.Transfer) {
                bankAccountTransactionDeleted(user, transaction)
            }

            if (transaction.type == TransactionType.CreditCard) {
                creditCardTransactionDeleted(user, transaction)
                creditCardService.addToAvailableLimit(user, transaction.creditCardId!!, transaction.value.negate())
            }
        }
    }

    private fun applyBankDtoIntoEntity(transaction: Transaction, dto: NewBankTransactionDto) {
        val bank = entityManager.getReference(BankAccount::class.java, dto.bankAccountId)

        transaction.bankAccount = bank
        transaction.bankAccountId = dto.bankAccountId
    }

    private fun applyCreditCardDtoIntoEntity(transaction: Transaction, dto: INewCreditCardTransactionDto) {
        val creditCard = entityManager.getReference(CreditCard::class.java, dto.creditCardId)
        val creditCardBillDate = creditCardBillService.getOrCreate(dto.creditCardBillDateValue, creditCard)

        transaction.creditCard = creditCard
        transaction.creditCardId = dto.categoryId
        transaction.creditCardBillDate = creditCardBillDate
        transaction.totalInstallments = dto.totalInstallments
    }

    private fun applyTransferDtoIntoEntity(transaction: Transaction, dto: NewTransferTransactionDto) {
        if (dto.bankAccountId == dto.bankAccount2Id) {
            throw SFException(
                reason = "The bank account can't be the same on transfer."
            )
        }

        val user2 = entityManager.getReference(User::class.java, dto.secondUserId)
        val bank2 = entityManager.getReference(BankAccount::class.java, dto.bankAccount2Id)

        val otherSideTransaction = Transaction(
            type = transaction.type,
            category = transaction.category,
            group = transaction.group,
            user = user2,
            bankAccount = bank2,
            date = transaction.date,
            value = transaction.value,
            description = transaction.description,
        )

        transactionRepository.save(otherSideTransaction)

        transaction.otherSide = otherSideTransaction
    }

    private fun findOne(
        id: Long, user: User, groupId: Long?
    ): Transaction? {
        if (groupId != null && !groupService.userHasPermissionToGroup(user, groupId)) {
            throw SFExceptionForbidden()
        }

        return transactionRepository.findOne(
            id = id, userId = if (groupId == null) user.id!! else null, groupId = groupId
        )
    }

    private fun bankAccountTransactionCreated(user: User, transaction: Transaction) {
        val bankAccountId = transaction.bankAccountId!!
        simpMessagingTemplate.convertAndSendToUser(
            user.email,
            "/queue/bank-account/transaction-created/${bankAccountId}",
            transactionMapper.toDto(transaction)!!
        )
        simpMessagingTemplate.convertAndSendToUser(
            user.email, "/queue/bank-account/transaction-created", transactionMapper.toDto(transaction)!!
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
            user.email, "/queue/bank-account/transaction-updated", transactionMapper.toDto(transaction)!!
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
            user.email, "/queue/bank-account/transaction-deleted", transactionMapper.toDto(transaction)!!
        )
    }

    private fun creditCardTransactionCreated(user: User, transaction: Transaction) {
        val creditCardId = transaction.creditCardId!!
        simpMessagingTemplate.convertAndSendToUser(
            user.email, "/queue/credit-card/transaction-created/${creditCardId}", transactionMapper.toDto(transaction)!!
        )
    }

    private fun creditCardTransactionUpdated(user: User, transaction: Transaction) {
        val creditCardId = transaction.creditCardId!!
        simpMessagingTemplate.convertAndSendToUser(
            user.email, "/queue/credit-card/transaction-updated/${creditCardId}", transactionMapper.toDto(transaction)!!
        )
    }

    private fun creditCardTransactionDeleted(user: User, transaction: Transaction) {
        val creditCardId = transaction.creditCardId!!
        simpMessagingTemplate.convertAndSendToUser(
            user.email, "/queue/credit-card/transaction-deleted/${creditCardId}", transactionMapper.toDto(transaction)!!
        )
    }
}
