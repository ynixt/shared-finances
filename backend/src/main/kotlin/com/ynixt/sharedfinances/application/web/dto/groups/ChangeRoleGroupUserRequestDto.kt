package com.ynixt.sharedfinances.application.web.dto.groups

import com.ynixt.sharedfinances.domain.enums.UserGroupRole
import java.util.UUID

data class ChangeRoleGroupUserRequestDto(
    val memberId: UUID,
    val role: UserGroupRole,
)
