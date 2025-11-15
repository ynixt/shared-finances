package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.user.UserResponseDto
import com.ynixt.sharedfinances.application.web.dto.user.UserSimpleDto
import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.models.security.UserPrincipal

interface UserDtoMapper {
    fun toDto(from: UserEntity): UserResponseDto

    fun tSimpleDto(from: UserEntity): UserSimpleDto

    fun toResponseDtoFromPrincipal(from: UserPrincipal): UserResponseDto
}
