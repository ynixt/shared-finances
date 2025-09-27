package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.user.UserResponseDto
import com.ynixt.sharedfinances.application.web.dto.user.UserSimpleDto
import com.ynixt.sharedfinances.application.web.mapper.UserDtoMapper
import com.ynixt.sharedfinances.domain.entities.User
import com.ynixt.sharedfinances.domain.models.security.UserPrincipal
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
class UserDtoMapperImpl : UserDtoMapper {
    override fun toDto(from: User): UserResponseDto = UserResponseMapper.map(from)

    override fun tSimpleDto(from: User): UserSimpleDto = UserSimpleDtoMapper.map(from)

    override fun toResponseDtoFromPrincipal(from: UserPrincipal): UserResponseDto = UserPrincipalToUserResponseMapper.map(from)

    private object UserResponseMapper : ObjectMappie<User, UserResponseDto>() {
        override fun map(from: User) =
            mapping {
                to::id fromPropertyNotNull from::id
            }
    }

    private object UserSimpleDtoMapper : ObjectMappie<User, UserSimpleDto>() {
        override fun map(from: User) =
            mapping {
                to::id fromPropertyNotNull from::id
            }
    }

    private object UserPrincipalToUserResponseMapper : ObjectMappie<UserPrincipal, UserResponseDto>() {
        override fun map(from: UserPrincipal) = mapping {}
    }
}
