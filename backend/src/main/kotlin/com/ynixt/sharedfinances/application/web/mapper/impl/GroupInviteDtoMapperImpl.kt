package com.ynixt.sharedfinances.application.web.mapper.impl

import com.ynixt.sharedfinances.application.web.dto.groups.GroupInviteDto
import com.ynixt.sharedfinances.application.web.dto.groups.invite.GroupInfoForInviteDto
import com.ynixt.sharedfinances.application.web.mapper.GroupInviteDtoMapper
import com.ynixt.sharedfinances.domain.entities.GroupInvite
import com.ynixt.sharedfinances.domain.models.groups.GroupInfoForInvite
import org.springframework.stereotype.Component
import tech.mappie.api.ObjectMappie

@Component
class GroupInviteDtoMapperImpl : GroupInviteDtoMapper {
    override fun toDto(from: GroupInvite): GroupInviteDto = GroupInviteToDtoMapper.map(from)

    override fun toDto(from: GroupInfoForInvite): GroupInfoForInviteDto = GroupInfoForInviteDtoMapper.map(from)

    private object GroupInviteToDtoMapper : ObjectMappie<GroupInvite, GroupInviteDto>() {
        override fun map(from: GroupInvite) =
            mapping {
                to::id fromPropertyNotNull from::id
            }
    }

    private object GroupInfoForInviteDtoMapper : ObjectMappie<GroupInfoForInvite, GroupInfoForInviteDto>() {
        override fun map(from: GroupInfoForInvite) = mapping {}
    }
}
