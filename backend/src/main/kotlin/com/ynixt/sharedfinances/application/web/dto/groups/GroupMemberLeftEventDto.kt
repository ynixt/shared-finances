package com.ynixt.sharedfinances.application.web.dto.groups

import java.util.UUID

data class GroupMemberLeftEventDto(
    val groupId: UUID,
    val userId: UUID,
)
