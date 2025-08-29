package com.ynixt.sharedfinances.domain.entities.wallet.entries

import com.ynixt.sharedfinances.domain.entities.AuditedEntity
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("entry_ratio_config")
class EntryRatioConfig(
    val entryId: UUID,
    val userId: UUID,
    val ratio: Double,
    val paid: Boolean,
) : AuditedEntity()
