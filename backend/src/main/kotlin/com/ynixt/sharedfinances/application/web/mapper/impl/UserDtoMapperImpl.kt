package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.UserResponseDto
import com.ynixt.sharedfinances.application.web.mapper.UserDtoMapper
import com.ynixt.sharedfinances.domain.models.security.UserPrincipal
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
class UserDtoMapperImpl : UserDtoMapper {
    override fun toResponseDtoFromPrincipal(from: UserPrincipal): UserResponseDto = UserPrincipalToUserResponseMapper.map(from)

    private object UserPrincipalToUserResponseMapper : ObjectMappie<UserPrincipal, UserResponseDto>() {
        override fun map(from: UserPrincipal) = mapping {}
    }
}
