package com.ynixt.sharedfinances.controllers.ws

import com.ynixt.sharedfinances.mapper.TransactionCategoryMapper
import com.ynixt.sharedfinances.model.dto.transactioncategory.GroupTransactionCategoryDto
import com.ynixt.sharedfinances.service.SecurityService
import com.ynixt.sharedfinances.service.TransactionCategoryService
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.stereotype.Controller
import java.security.Principal

@Controller
class GroupTransactionCategoryWSController(
    private val transactionCategoryService: TransactionCategoryService,
    private val transactionCategoryMapper: TransactionCategoryMapper,
    private val securityService: SecurityService
) {
    @MessageMapping("/group-transaction-category/{groupId}")
    @SendTo("/topic/group-transaction-category/{groupId}")
    fun listTransactionCategories(
        principal: Principal, @DestinationVariable groupId: Long
    ): List<GroupTransactionCategoryDto> {
        return transactionCategoryMapper.toDtoGroupList(
            transactionCategoryService.findAllGroupCategories(
                securityService.principalToUser(
                    principal
                )!!, groupId
            )
        )!!
    }
}
