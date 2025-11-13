package com.ynixt.sharedfinances.domain.models

import com.ynixt.sharedfinances.domain.entities.User
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import java.util.UUID

data class WalletItemSearchResponse(
    val id: UUID,
    val name: String,
    val user: User?,
    val currency: String,
    val type: WalletItemType,
)
