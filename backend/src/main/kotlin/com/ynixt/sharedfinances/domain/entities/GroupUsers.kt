package com.ynixt.sharedfinances.domain.entities

import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("group_users")
class GroupUsers(
    val groupId: UUID,
    val userId: UUID,
)
