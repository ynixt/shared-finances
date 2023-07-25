package com.ynixt.sharedfinances.controllers.ws

import com.ynixt.sharedfinances.model.dto.transactioncategory.UserTransactionCategoryDto
import com.ynixt.sharedfinances.service.SecurityService
import com.ynixt.sharedfinances.service.TransactionCategoryService
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.stereotype.Controller
import java.security.Principal

@Controller
class UserTransactionCategoryWSController(
    private val transactionCategoryService: TransactionCategoryService,
    private val securityService: SecurityService
) {
    @MessageMapping("/user-transaction-category")
    @SendToUser("/queue/user-transaction-category")
    fun listTransactionCategories(principal: Principal): List<UserTransactionCategoryDto> {
        return transactionCategoryService.findAllUserCategoriesAsUserTransactionCategoryDto(
            securityService.principalToUser(
                principal
            )!!
        )
    }
}
