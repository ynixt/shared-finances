package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.groups.GroupInviteDto
import com.ynixt.sharedfinances.application.web.dto.groups.invite.GroupInfoForInviteDto
import com.ynixt.sharedfinances.domain.entities.GroupInvite
import com.ynixt.sharedfinances.domain.models.groups.GroupInfoForInvite

interface GroupInviteDtoMapper {
    fun toDto(from: GroupInvite): GroupInviteDto

    fun toDto(from: GroupInfoForInvite): GroupInfoForInviteDto
}
