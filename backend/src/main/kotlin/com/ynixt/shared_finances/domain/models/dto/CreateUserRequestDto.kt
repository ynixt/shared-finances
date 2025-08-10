package com.ynixt.shared_finances.domain.models.dto

data class CreateUserRequestDto(
    val uid: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val photoUrl: String? = null,
    val lang: String,
)
