package com.ynixt.sharedfinances.controllers.ws

import com.ynixt.sharedfinances.model.dto.creditcard.CreditCardDto
import com.ynixt.sharedfinances.service.CreditCardService
import com.ynixt.sharedfinances.service.SecurityService
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.stereotype.Controller
import java.security.Principal

@Controller
class CreditCardWSController(
    private val creditCardService: CreditCardService,
    private val securityService: SecurityService
) {
    @MessageMapping("/credit-card")
    @SendToUser("/queue/credit-card")
    fun listCreditCards(principal: Principal): List<CreditCardDto> {
        return creditCardService.listCreditCardAsCreditCardDto(securityService.principalToUser(principal)!!)
    }
}
