package com.ynixt.sharedfinances.domain.entities.wallet

import com.ynixt.sharedfinances.domain.entities.AuditedEntity
import java.util.UUID

abstract class WalletItem(
    val name: String,
    val enabled: Boolean,
    val userId: UUID,
) : AuditedEntity()
