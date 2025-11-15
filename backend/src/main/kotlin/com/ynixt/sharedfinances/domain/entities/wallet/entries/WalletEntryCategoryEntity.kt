package com.ynixt.sharedfinances.domain.entities.wallet.entries

import com.ynixt.sharedfinances.domain.entities.AuditedEntity
import org.springframework.data.annotation.Transient
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("wallet_entry_category")
class WalletEntryCategoryEntity(
    val name: String,
    val color: String,
    val userId: UUID?,
    val groupId: UUID?,
    val parentId: UUID?,
) : AuditedEntity() {
    @Transient
    var children: List<WalletEntryCategoryEntity>? = null
}
