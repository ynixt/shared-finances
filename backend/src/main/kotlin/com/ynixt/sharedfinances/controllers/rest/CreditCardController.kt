package com.ynixt.sharedfinances.controllers.rest

import com.ynixt.sharedfinances.mapper.CreditCardMapper
import com.ynixt.sharedfinances.model.dto.TransactionValuesAndDateDto
import com.ynixt.sharedfinances.model.dto.creditcard.*
import com.ynixt.sharedfinances.model.dto.transaction.TransactionDto
import com.ynixt.sharedfinances.service.CreditCardService
import com.ynixt.sharedfinances.service.SecurityService
import com.ynixt.sharedfinances.service.TransactionService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/credit-card")
class CreditCardController(
    private val creditCardService: CreditCardService,
    private val securityService: SecurityService,
    private val creditCardMapper: CreditCardMapper,
    private val transactionService: TransactionService
) {
    @GetMapping("summary/{creditCardId}/{maxCreditCardBillDate}")
    fun summary(
        authentication: Authentication, @PathVariable creditCardId: Long, @PathVariable maxCreditCardBillDate: LocalDate
    ): CreditCardSummaryDto {
        val user = securityService.authenticationToUser(authentication)!!
        return this.creditCardService.getSummary(user, creditCardId, maxCreditCardBillDate)
    }

    @GetMapping("{id}")
    fun getOne(authentication: Authentication, @PathVariable id: Long): ResponseEntity<CreditCardDto> {
        val user = securityService.authenticationToUser(authentication)!!

        return ResponseEntity.ofNullable(creditCardMapper.toDto(creditCardService.getOne(user, id)))
    }

    @GetMapping("{id}/limit")
    fun getCurrentLimit(authentication: Authentication, @PathVariable id: Long): ResponseEntity<CreditCardLimitDto> {
        val user = securityService.authenticationToUser(authentication)!!

        return ResponseEntity.ofNullable(creditCardService.getCurrentLimit(user, id))
    }

    @PutMapping("{id}")
    fun update(
        authentication: Authentication, @PathVariable id: Long, @RequestBody updateDto: UpdateCreditCardDto
    ): CreditCardDto {
        val user = securityService.authenticationToUser(authentication)!!

        return creditCardMapper.toDto(creditCardService.updateCreditCard(user, id, updateDto))!!
    }

    @PostMapping
    fun newCreditCard(authentication: Authentication, @RequestBody newCreditCardDto: NewCreditCardDto): CreditCardDto {
        val user = securityService.authenticationToUser(authentication)!!

        return creditCardMapper.toDto(creditCardService.newCreditCard(user, newCreditCardDto))!!
    }

    @DeleteMapping("{id}")
    fun delete(authentication: Authentication, @PathVariable("id") id: Long) {
        val user = securityService.authenticationToUser(authentication)!!

        return creditCardService.delete(user, id)
    }

    @GetMapping("{creditCardId}/transactions")
    fun listTransactions(
        authentication: Authentication,
        @PathVariable creditCardId: Long,
        @RequestParam minDate: LocalDate?,
        @RequestParam maxDate: LocalDate?,
        @RequestParam creditCardBillDate: LocalDate?,
        pageable: Pageable
    ): Page<TransactionDto> {
        val user = securityService.authenticationToUser(authentication)!!

        return transactionService.findAllIncludeGroupAndCategoryAsTransactionDto(
            bankAccountId = null,
            groupId = null,
            creditCardId = creditCardId,
            user = user,
            minDate = minDate,
            maxDate = maxDate,
            creditCardBillDate = creditCardBillDate,
            pageable = pageable
        )
    }

    @GetMapping("{creditCardId}/chart")
    fun getChartByBankAccountId(
        authentication: Authentication,
        @PathVariable creditCardId: Long,
        @RequestParam minCreditCardBillDate: LocalDate?,
        @RequestParam maxCreditCardBillDate: LocalDate?,
    ): List<TransactionValuesAndDateDto> {
        val user = securityService.authenticationToUser(authentication)!!

        return creditCardService.getChartByCreditCardId(
            user = user,
            creditCardId = creditCardId,
            minCreditCardBillDate = minCreditCardBillDate,
            maxCreditCardBillDate = maxCreditCardBillDate
        )
    }
}
