package com.ynixt.sharedfinances.application.web.dto.user

import java.util.UUID

data class UserSimpleDto(
    val id: UUID,
    val firstName: String,
    var lastName: String,
    val email: String,
)
