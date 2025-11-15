package com.ynixt.sharedfinances.domain.entities

import org.springframework.data.relational.core.mapping.Table

@Table("users")
class UserEntity(
    val externalId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    var lang: String,
    var defaultCurrency: String?,
) : AuditedEntity()
