package com.ynixt.sharedfinances.controllers.ws

import com.ynixt.sharedfinances.mapper.BankAccountMapper
import com.ynixt.sharedfinances.model.dto.bankAccount.BankAccountDto
import com.ynixt.sharedfinances.service.BankAccountService
import com.ynixt.sharedfinances.service.SecurityService
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.stereotype.Controller
import java.security.Principal

@Controller
class BankAccountWSController(
    private val bankAccountService: BankAccountService,
    private val securityService: SecurityService,
    private val bankAccountMapper: BankAccountMapper
) {
    @MessageMapping("/bank-account/{bankAccountId}")
    @SendToUser("/queue/bank-account/{bankAccountId}")
    fun listTransactions(
        principal: Principal,
        @DestinationVariable bankAccountId: Long,
    ): BankAccountDto? {
        val user = securityService.principalToUser(principal)!!

        return bankAccountMapper.toDto(bankAccountService.getOne(bankAccountId, user))
    }
}
