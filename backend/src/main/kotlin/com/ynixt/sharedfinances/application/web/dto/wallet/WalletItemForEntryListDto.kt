package com.ynixt.sharedfinances.application.web.dto.wallet

import com.ynixt.sharedfinances.application.web.dto.user.UserSimpleDto
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import java.util.UUID

data class WalletItemForEntryListDto(
    val id: UUID,
    val name: String,
    val enabled: Boolean,
    val user: UserSimpleDto?,
    val currency: String,
    val type: WalletItemType,
)
