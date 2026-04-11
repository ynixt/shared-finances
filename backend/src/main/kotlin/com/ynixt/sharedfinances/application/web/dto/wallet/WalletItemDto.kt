package com.ynixt.sharedfinances.application.web.dto.wallet

import java.util.UUID

abstract class WalletItemDto(
    val id: UUID,
    val name: String,
    val enabled: Boolean,
    val showOnDashboard: Boolean = true,
    val userId: UUID,
    val currency: String,
)
