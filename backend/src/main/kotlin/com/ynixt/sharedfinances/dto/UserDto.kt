package com.ynixt.sharedfinances.dto

data class UserDto(
    val id: Long? = null,
    val email: String,
    val name: String,
    val photoUrl: String?
)