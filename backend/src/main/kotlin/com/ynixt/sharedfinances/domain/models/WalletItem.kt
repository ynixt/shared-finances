package com.ynixt.sharedfinances.domain.models

import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.enums.WalletItemType
import java.math.BigDecimal
import java.util.UUID

abstract class WalletItem(
    val name: String,
    val enabled: Boolean,
    val userId: UUID,
    val currency: String,
    val balance: BigDecimal,
    val showOnDashboard: Boolean = true,
) : AuditedEntityModel() {
    var user: UserEntity? = null
    abstract val type: WalletItemType
}
