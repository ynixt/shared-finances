package com.ynixt.sharedfinances.model.dto.user

data class UserDto(
    val id: Long? = null,
    val email: String,
    val name: String,
    val photoUrl: String?
)
