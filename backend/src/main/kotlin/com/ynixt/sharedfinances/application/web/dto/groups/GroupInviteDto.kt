package com.ynixt.sharedfinances.application.web.dto.groups

import java.time.OffsetDateTime
import java.util.UUID

data class GroupInviteDto(
    val id: UUID,
    val groupId: UUID,
    val expireAt: OffsetDateTime,
)
