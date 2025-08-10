package com.ynixt.shared_finances.domain.entities

import org.springframework.data.relational.core.mapping.Table

@Table("group")
class Group(
    val name: String,
) : AuditedEntity() {
}