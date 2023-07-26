package com.ynixt.sharedfinances.controllers.rest

import com.ynixt.sharedfinances.mapper.CreditCardMapper
import com.ynixt.sharedfinances.model.dto.creditcard.CreditCardDto
import com.ynixt.sharedfinances.model.dto.creditcard.CreditCardSummaryDto
import com.ynixt.sharedfinances.model.dto.creditcard.NewCreditCardDto
import com.ynixt.sharedfinances.model.dto.creditcard.UpdateCreditCardDto
import com.ynixt.sharedfinances.service.CreditCardService
import com.ynixt.sharedfinances.service.SecurityService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/credit-card")
class CreditCardController(
    private val creditCardService: CreditCardService,
    private val securityService: SecurityService,
    private val creditCardMapper: CreditCardMapper
) {
    @GetMapping("summary/{creditCardId}/{maxCreditCardBillDate}")
    fun summary(
        authentication: Authentication,
        @PathVariable creditCardId: Long,
        @PathVariable maxCreditCardBillDate: LocalDate
    ): CreditCardSummaryDto {
        val user = securityService.authenticationToUser(authentication)!!
        return this.creditCardService.getSummary(user, creditCardId, maxCreditCardBillDate)
    }

    @GetMapping("{id}")
    fun getOne(authentication: Authentication, @PathVariable id: Long): ResponseEntity<CreditCardDto> {
        val user = securityService.authenticationToUser(authentication)!!

        return ResponseEntity.ofNullable(creditCardMapper.toDto(creditCardService.getOne(user, id)))
    }

    @PutMapping("{id}")
    fun update(
        authentication: Authentication,
        @PathVariable id: Long,
        @RequestBody updateDto: UpdateCreditCardDto
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
}
