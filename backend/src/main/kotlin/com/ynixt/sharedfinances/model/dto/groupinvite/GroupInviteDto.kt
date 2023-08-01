package com.ynixt.sharedfinances.model.dto.groupinvite

import java.time.OffsetDateTime
import java.util.*

data class GroupInviteDto(
    val code: UUID,
    val expiresOn: OffsetDateTime,
    val groupId: Long
)
