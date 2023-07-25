package com.ynixt.sharedfinances.model.dto.group

import com.ynixt.sharedfinances.model.dto.user.UserDto

data class GroupViewDto(
    val id: Long? = null,
    val name: String,
    val users: List<UserDto>
)
