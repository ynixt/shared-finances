package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.user.UserResponseDto
import com.ynixt.sharedfinances.application.web.dto.user.UserSimpleDto
import com.ynixt.sharedfinances.domain.entities.User
import com.ynixt.sharedfinances.domain.models.security.UserPrincipal

interface UserDtoMapper {
    fun toDto(from: User): UserResponseDto

    fun tSimpleDto(from: User): UserSimpleDto

    fun toResponseDtoFromPrincipal(from: UserPrincipal): UserResponseDto
}
