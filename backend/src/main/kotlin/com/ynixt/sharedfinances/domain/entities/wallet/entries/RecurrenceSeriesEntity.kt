package com.ynixt.sharedfinances.domain.entities.wallet.entries

import com.ynixt.sharedfinances.domain.entities.AuditedEntity
import org.springframework.data.relational.core.mapping.Table

@Table("recurrence_series")
class RecurrenceSeriesEntity(
    val qtyTotal: Int?,
) : AuditedEntity()
