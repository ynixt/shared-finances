package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.UserResponseDto
import com.ynixt.sharedfinances.domain.entities.User
import com.ynixt.sharedfinances.domain.models.security.UserPrincipal

interface UserDtoMapper {
    fun toDto(from: User): UserResponseDto

    fun toResponseDtoFromPrincipal(from: UserPrincipal): UserResponseDto
}
