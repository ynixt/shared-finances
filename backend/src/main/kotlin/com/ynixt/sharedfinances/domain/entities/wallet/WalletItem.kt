package com.ynixt.sharedfinances.domain.entities.wallet

import com.ynixt.sharedfinances.domain.entities.AuditedEntity
import com.ynixt.sharedfinances.domain.entities.User
import org.springframework.data.annotation.Transient
import java.util.UUID

abstract class WalletItem(
    val name: String,
    val enabled: Boolean,
    val userId: UUID,
    val currency: String,
) : AuditedEntity() {
    @Transient
    var user: User? = null
}
