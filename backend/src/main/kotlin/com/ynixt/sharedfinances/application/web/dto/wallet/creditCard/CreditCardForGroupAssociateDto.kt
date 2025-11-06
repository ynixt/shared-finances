package com.ynixt.sharedfinances.application.web.dto.wallet.creditCard

import com.ynixt.sharedfinances.application.web.dto.user.UserSimpleDto
import java.util.UUID

data class CreditCardForGroupAssociateDto(
    val id: UUID,
    val name: String,
    val user: UserSimpleDto,
    val currency: String,
)
