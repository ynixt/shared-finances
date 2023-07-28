package com.ynixt.sharedfinances.service.impl

import com.ynixt.sharedfinances.entity.BankAccount
import com.ynixt.sharedfinances.entity.User
import com.ynixt.sharedfinances.model.dto.TransactionValuesAndDateDto
import com.ynixt.sharedfinances.model.dto.bankAccount.BankAccountSummaryDto
import com.ynixt.sharedfinances.model.dto.bankAccount.NewBankAccountDto
import com.ynixt.sharedfinances.model.exceptions.SFException
import com.ynixt.sharedfinances.repository.BankAccountRepository
import com.ynixt.sharedfinances.repository.TransactionRepository
import com.ynixt.sharedfinances.service.BankAccountService
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BankAccountServiceImpl(
    private val bankAccountRepository: BankAccountRepository,
    private val transactionRepository: TransactionRepository
) : BankAccountService {
    override fun getOne(id: Long, user: User): BankAccount? {
        return bankAccountRepository.findById(id).orElse(null)
    }

    override fun updateName(id: Long, user: User, newName: String): BankAccount {
        val bankAccount = getOne(id, user) ?: throw SFException(
            reason = "Bank account not found"
        )

        bankAccount.name = newName
        return bankAccountRepository.save(bankAccount)
    }

    @Transactional
    override fun deleteOne(id: Long, user: User) {
        bankAccountRepository.deleteByIdAndUserId(id = id, userId = user.id!!)
    }


    override fun getSummary(user: User, bankAccountId: Long?, maxDate: LocalDate?): BankAccountSummaryDto {
        return transactionRepository.getBankAccountSummary(user.id!!, bankAccountId, maxDate)
    }

    @Transactional
    override fun newBank(user: User, newBankAccountDto: NewBankAccountDto): BankAccount {
        var bankAccount = BankAccount(
            name = newBankAccountDto.name,
            enabled = newBankAccountDto.enabled,
            displayOnGroup = newBankAccountDto.displayOnGroup,
            user = user
        )

        bankAccount = bankAccountRepository.save(bankAccount)

        return bankAccount
    }

    override fun getChartByBankAccountId(
        user: User,
        bankAccountId: Long,
        minDate: LocalDate?,
        maxDate: LocalDate?
    ): List<TransactionValuesAndDateDto> {
        return transactionRepository.findAllByBankAccountIdGroupedByDate(
            userId = user.id!!,
            bankAccountId = bankAccountId,
            minDate = minDate ?: LocalDate.now(),
            maxDate = maxDate ?: LocalDate.now().plusDays(1)
        )
    }
}
