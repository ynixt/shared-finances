package com.ynixt.sharedfinances.controllers.ws

import com.ynixt.sharedfinances.mapper.TransactionCategoryMapper
import com.ynixt.sharedfinances.model.dto.transactioncategory.TransactionCategoryDto
import com.ynixt.sharedfinances.service.SecurityService
import com.ynixt.sharedfinances.service.TransactionCategoryService
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.stereotype.Controller
import java.security.Principal

@Controller
class TransactionCategoryWSController(
    private val transactionCategoryService: TransactionCategoryService,
    private val transactionCategoryMapper: TransactionCategoryMapper,
    private val securityService: SecurityService
) {
    @MessageMapping("/transaction-category")
    @SendToUser("/queue/transaction-category")
    fun listTransactionCategories(principal: Principal): List<TransactionCategoryDto> {
        return transactionCategoryMapper.toDtoList(
            transactionCategoryService.findAll(
                securityService.principalToUser(
                    principal
                )!!
            )
        )
    }
}
