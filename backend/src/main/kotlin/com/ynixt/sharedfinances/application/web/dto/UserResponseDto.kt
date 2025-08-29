package com.ynixt.sharedfinances.application.web.dto

import java.util.UUID

data class UserResponseDto(
    val id: UUID,
    val externalId: String,
    val email: String,
    val firstName: String,
    var lastName: String,
    val photoUrl: String?,
    val lang: String,
    val defaultCurrency: String?,
)
