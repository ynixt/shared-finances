package com.ynixt.sharedfinances.application.web.dto.groups

import java.util.UUID

data class TransferGroupOwnershipDto(
    val newOwnerId: UUID,
)
