package com.ynixt.sharedfinances.domain.entities.groups

import com.ynixt.sharedfinances.domain.entities.SimpleEntity
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("group_invite")
class GroupInvite(
    val groupId: UUID,
    val expireAt: OffsetDateTime,
) : SimpleEntity()
