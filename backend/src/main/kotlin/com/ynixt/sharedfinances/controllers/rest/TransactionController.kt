package com.ynixt.sharedfinances.controllers.rest

import com.ynixt.sharedfinances.mapper.TransactionMapper
import com.ynixt.sharedfinances.model.dto.transaction.TransactionDto
import com.ynixt.sharedfinances.service.SecurityService
import com.ynixt.sharedfinances.service.TransactionService
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/transaction")
class TransactionController(
    private val securityService: SecurityService,
    private val transactionService: TransactionService,
    private val transactionMapper: TransactionMapper
) {
    @PostMapping
    fun newTransaction(authentication: Authentication, @RequestBody body: TransactionDto): TransactionDto {
        val user = securityService.authenticationToUser(authentication)!!
        return transactionMapper.toDto(transactionService.newTransaction(user, body))!!
    }
}
