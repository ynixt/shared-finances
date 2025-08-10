package com.ynixt.shared_finances.domain.entities

import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.*

@Table("group_invite")
class GroupInvite(
    val groupId: UUID,
    val expireAt: OffsetDateTime
) {
}