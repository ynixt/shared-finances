package com.ynixt.sharedfinances.application.web.dto.groups

import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import java.util.UUID

data class GroupWithRoleDto(
    val id: UUID,
    val name: String,
    val role: UserGroupRole,
)
