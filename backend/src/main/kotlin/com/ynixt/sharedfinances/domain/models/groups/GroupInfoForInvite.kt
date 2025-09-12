package com.ynixt.sharedfinances.domain.models.groups

import java.time.OffsetDateTime
import java.util.UUID

data class GroupInfoForInvite(
    val id: UUID,
    val groupName: String,
    val expireAt: OffsetDateTime,
)
