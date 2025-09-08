package com.ynixt.sharedfinances.application.web.dto.groups

import com.ynixt.sharedfinances.application.web.dto.UserResponseDto
import com.ynixt.sharedfinances.domain.enums.UserGroupRole

data class GroupUserDto(
    val role: UserGroupRole,
    val user: UserResponseDto,
)
