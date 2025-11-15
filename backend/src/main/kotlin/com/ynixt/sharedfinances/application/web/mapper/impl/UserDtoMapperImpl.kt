package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.user.UserResponseDto
import com.ynixt.sharedfinances.application.web.dto.user.UserSimpleDto
import com.ynixt.sharedfinances.application.web.mapper.UserDtoMapper
import com.ynixt.sharedfinances.domain.entities.UserEntity
import com.ynixt.sharedfinances.domain.models.security.UserPrincipal
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
class UserDtoMapperImpl : UserDtoMapper {
    override fun toDto(from: UserEntity): UserResponseDto = UserResponseMapper.map(from)

    override fun tSimpleDto(from: UserEntity): UserSimpleDto = UserSimpleDtoMapper.map(from)

    override fun toResponseDtoFromPrincipal(from: UserPrincipal): UserResponseDto = UserPrincipalToUserResponseMapper.map(from)

    private object UserResponseMapper : ObjectMappie<UserEntity, UserResponseDto>() {
        override fun map(from: UserEntity) =
            mapping {
                to::id fromPropertyNotNull from::id
            }
    }

    private object UserSimpleDtoMapper : ObjectMappie<UserEntity, UserSimpleDto>() {
        override fun map(from: UserEntity) =
            mapping {
                to::id fromPropertyNotNull from::id
            }
    }

    private object UserPrincipalToUserResponseMapper : ObjectMappie<UserPrincipal, UserResponseDto>() {
        override fun map(from: UserPrincipal) = mapping {}
    }
}
