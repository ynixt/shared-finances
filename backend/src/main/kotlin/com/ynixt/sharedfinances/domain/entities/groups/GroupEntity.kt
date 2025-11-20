package com.ynixt.sharedfinances.domain.entities.groups

import com.ynixt.sharedfinances.domain.entities.AuditedEntity
import org.springframework.data.relational.core.mapping.Table

@Table("\"group\"")
class GroupEntity(
    val name: String,
) : AuditedEntity()
