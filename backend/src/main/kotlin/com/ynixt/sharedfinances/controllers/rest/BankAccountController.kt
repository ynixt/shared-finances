package com.ynixt.sharedfinances.controllers.rest

import com.ynixt.sharedfinances.mapper.BankAccountMapper
import com.ynixt.sharedfinances.model.dto.TransactionValuesAndDateDto
import com.ynixt.sharedfinances.model.dto.bankAccount.BankAccountDto
import com.ynixt.sharedfinances.model.dto.bankAccount.BankAccountSummaryDto
import com.ynixt.sharedfinances.model.dto.bankAccount.NewBankAccountDto
import com.ynixt.sharedfinances.model.dto.transaction.TransactionDto
import com.ynixt.sharedfinances.service.BankAccountService
import com.ynixt.sharedfinances.service.SecurityService
import com.ynixt.sharedfinances.service.TransactionService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/bank-account")
class BankAccountController(
    private val bankAccountService: BankAccountService,
    private val securityService: SecurityService,
    private val bankAccountMapper: BankAccountMapper,
    private val transactionService: TransactionService
) {
    @GetMapping("{id}")
    fun getOne(authentication: Authentication, @PathVariable("id") id: Long): BankAccountDto? {
        val user = securityService.authenticationToUser(authentication)!!

        return bankAccountMapper.toDto(bankAccountService.getOne(id, user))
    }

    @PutMapping("{id}/{newName}")
    fun updateName(
        authentication: Authentication, @PathVariable("id") id: Long, @PathVariable("newName") newName: String
    ): BankAccountDto {
        val user = securityService.authenticationToUser(authentication)!!
        return bankAccountMapper.toDto(bankAccountService.updateName(id, user, newName))!!
    }

    @DeleteMapping("{id}")
    fun delete(authentication: Authentication, @PathVariable("id") id: Long) {
        val user = securityService.authenticationToUser(authentication)!!
        bankAccountService.deleteOne(id, user)
    }

    @GetMapping("summary")
    fun getBankAccountSummary(
        authentication: Authentication,
        @RequestParam("bankAccountId", required = false) bankAccountId: Long?,
        @RequestParam("maxDate", required = false) maxDate: LocalDate?
    ): BankAccountSummaryDto {
        val user = securityService.authenticationToUser(authentication)!!
        return bankAccountService.getSummary(user, bankAccountId, maxDate)
    }

    @PostMapping
    fun newBankAccount(
        authentication: Authentication, @RequestBody newBankAccountDto: NewBankAccountDto
    ): BankAccountDto {
        val user = securityService.authenticationToUser(authentication)!!

        return bankAccountMapper.toDto(bankAccountService.newBank(user, newBankAccountDto))!!
    }

    @GetMapping("{bankAccountId}/transactions")
    fun listTransactions(
        authentication: Authentication,
        @PathVariable bankAccountId: Long,
        @RequestParam minDate: LocalDate?,
        @RequestParam maxDate: LocalDate?,
        pageable: Pageable
    ): Page<TransactionDto> {
        val user = securityService.authenticationToUser(authentication)!!

        return transactionService.findAllIncludeGroupAndCategoryAsTransactionDto(
            bankAccountId = bankAccountId,
            groupId = null,
            creditCardId = null,
            user = user,
            minDate = minDate,
            maxDate = maxDate,
            pageable = pageable
        )
    }

    @GetMapping("{bankAccountId}/chart")
    fun getChartByBankAccountId(
        authentication: Authentication,
        @PathVariable bankAccountId: Long,
        @RequestParam minDate: LocalDate?,
        @RequestParam maxDate: LocalDate?,
    ): List<TransactionValuesAndDateDto> {
        val user = securityService.authenticationToUser(authentication)!!

        return bankAccountService.getChartByBankAccountId(
            user = user,
            bankAccountId = bankAccountId,
            minDate = minDate,
            maxDate = maxDate
        )
    }
}
