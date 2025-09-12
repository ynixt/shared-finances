package com.ynixt.sharedfinances.application.web.dto.groups.invite

import java.time.OffsetDateTime
import java.util.UUID

data class GroupInfoForInviteDto(
    val id: UUID,
    val groupName: String,
    val expireAt: OffsetDateTime,
)
