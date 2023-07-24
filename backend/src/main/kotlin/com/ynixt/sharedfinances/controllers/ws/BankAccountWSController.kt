package com.ynixt.sharedfinances.controllers.ws

import com.ynixt.sharedfinances.service.BankAccountService
import org.springframework.stereotype.Controller

@Controller
class BankAccountWSController(
    private val bankAccountService: BankAccountService
) {
}