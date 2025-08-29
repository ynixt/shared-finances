package com.ynixt.sharedfinances.domain.entities.wallet.entries

import com.ynixt.sharedfinances.domain.entities.AuditedEntity
import org.springframework.data.relational.core.mapping.Table

@Table("wallet_entry_category")
class WalletEntryCategory(
    val name: String,
    val color: String,
    val icon: String,
) : AuditedEntity()
