package com.ynixt.sharedfinances.domain.models.groups

import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import java.time.OffsetDateTime
import java.util.UUID

data class GroupWithRole(
    var id: UUID?,
    var createdAt: OffsetDateTime?,
    var updatedAt: OffsetDateTime?,
    val name: String,
    val role: UserGroupRole,
)
