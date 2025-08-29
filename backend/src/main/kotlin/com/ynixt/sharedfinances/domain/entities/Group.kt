package com.ynixt.sharedfinances.domain.entities

import org.springframework.data.relational.core.mapping.Table

@Table("group")
class Group(
    val name: String,
) : AuditedEntity()
