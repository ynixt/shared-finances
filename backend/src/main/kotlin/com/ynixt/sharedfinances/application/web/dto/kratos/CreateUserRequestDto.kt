package com.ynixt.sharedfinances.application.web.dto.kratos

data class CreateUserRequestDto(
    val uid: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val photoUrl: String? = null,
    val lang: String,
    val defaultCurrency: String? = null,
)
