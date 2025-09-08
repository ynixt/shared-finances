package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.groups.GroupUserDto
import com.ynixt.sharedfinances.domain.entities.GroupUser

interface GroupUserDtoMapper {
    fun toDto(from: GroupUser): GroupUserDto
}
