package com.ynixt.sharedfinances.application.web.dto.wallet

import com.ynixt.sharedfinances.application.web.dto.user.UserSimpleDto
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import java.util.UUID

class WalletItemSearchResponseDto(
    val id: UUID,
    val name: String,
    val user: UserSimpleDto?,
    val currency: String,
    val type: WalletItemType,
    val dueDay: Int?,
    val dueOnNextBusinessDay: Boolean?,
)
