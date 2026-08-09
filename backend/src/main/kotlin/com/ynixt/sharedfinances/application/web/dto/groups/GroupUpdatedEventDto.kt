package com.ynixt.sharedfinances.application.web.dto.groups

import java.util.UUID

data class GroupUpdatedEventDto(
    val id: UUID,
    val name: String,
)
