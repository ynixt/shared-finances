package com.ynixt.sharedfinances.application.web.dto.groups

import java.util.UUID

data class GroupOwnershipChangedEventDto(
    val groupId: UUID,
    val previousOwnerUserId: UUID,
    val newOwnerUserId: UUID,
)
