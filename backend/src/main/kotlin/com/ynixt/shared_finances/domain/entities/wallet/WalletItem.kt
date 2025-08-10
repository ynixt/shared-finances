package com.ynixt.shared_finances.domain.entities.wallet

import com.ynixt.shared_finances.domain.entities.AuditedEntity
import java.util.UUID

abstract class WalletItem(
    val name: String,
    val enabled: Boolean,
    val userId: UUID,
): AuditedEntity()