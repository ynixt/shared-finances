package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.groups.GroupUserDto
import com.ynixt.sharedfinances.application.web.mapper.GroupUserDtoMapper
import com.ynixt.sharedfinances.application.web.mapper.UserDtoMapper
import com.ynixt.sharedfinances.domain.entities.GroupUser
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
class GroupUserDtoMapperImpl(
    private val userDtoMapper: UserDtoMapper,
) : GroupUserDtoMapper {
    private val groupUserToDtoMapper = GroupUserToDtoMapper(userDtoMapper)

    override fun toDto(from: GroupUser): GroupUserDto = groupUserToDtoMapper.map(from)

    private class GroupUserToDtoMapper(
        private val userDtoMapper: UserDtoMapper,
    ) : ObjectMappie<GroupUser, GroupUserDto>() {
        override fun map(from: GroupUser) =
            mapping {
                to::user fromPropertyNotNull from::user transform { userDtoMapper.toDto(it) }
            }
    }
}
