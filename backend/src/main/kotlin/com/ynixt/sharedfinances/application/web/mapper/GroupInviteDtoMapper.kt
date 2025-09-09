package com.ynixt.sharedfinances.application.web.mapper

import com.ynixt.sharedfinances.application.web.dto.groups.GroupInviteDto
import com.ynixt.sharedfinances.domain.entities.GroupInvite

interface GroupInviteDtoMapper {
    fun toDto(from: GroupInvite): GroupInviteDto
}
