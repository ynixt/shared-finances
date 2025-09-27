package com.ynixt.sharedfinances.application.web.dto.wallet.bankAccount

import com.ynixt.sharedfinances.application.web.dto.user.UserSimpleDto
import java.util.UUID

data class BankAccountForGroupAssociateDto(
    val id: UUID,
    val name: String,
    val user: UserSimpleDto,
    val currency: String,
)
