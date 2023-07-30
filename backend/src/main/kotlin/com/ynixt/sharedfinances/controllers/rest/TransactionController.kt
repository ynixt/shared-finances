package com.ynixt.sharedfinances.controllers.rest

import com.ynixt.sharedfinances.mapper.TransactionMapper
import com.ynixt.sharedfinances.model.dto.transaction.NewTransactionDto
import com.ynixt.sharedfinances.model.dto.transaction.TransactionDto
import com.ynixt.sharedfinances.service.SecurityService
import com.ynixt.sharedfinances.service.TransactionService
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/transaction")
class TransactionController(
    private val securityService: SecurityService,
    private val transactionService: TransactionService,
    private val transactionMapper: TransactionMapper
) {
    @PostMapping
    fun newTransaction(authentication: Authentication, @RequestBody body: NewTransactionDto): TransactionDto {
        val user = securityService.authenticationToUser(authentication)!!
        return transactionMapper.toDto(transactionService.newTransaction(user, body))!!
    }

    @PutMapping("{id}")
    fun edit(
        authentication: Authentication,
        @PathVariable("id") id: Long,
        @RequestBody body: NewTransactionDto
    ): TransactionDto {
        val user = securityService.authenticationToUser(authentication)!!

        return transactionMapper.toDto(transactionService.editTransaction(user, id, body))!!
    }

    @DeleteMapping("{id}")
    fun delete(
        authentication: Authentication,
        @PathVariable("id") id: Long,
        @RequestParam("groupId", required = false) groupId: Long?
    ) {
        val user = securityService.authenticationToUser(authentication)!!

        return transactionService.delete(user, id, groupId)
    }
}
