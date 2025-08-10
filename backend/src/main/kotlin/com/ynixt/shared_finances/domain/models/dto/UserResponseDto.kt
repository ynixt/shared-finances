package com.ynixt.shared_finances.domain.models.dto

import com.ynixt.shared_finances.domain.entities.User
import com.ynixt.shared_finances.domain.models.security.UserPrincipal

data class UserResponseDto(
    val id: String,
    val externalId: String,
    val email: String,
    val firstName: String,
    var lastName: String,
    val photoUrl: String?,
    val lang: String,
) {
    companion object {
        fun fromEntity(user: User) = UserResponseDto(
            id = user.id.toString(),
            externalId = user.externalId,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            photoUrl = user.photoUrl,
            lang = user.lang,
        )

        fun fromPrincipal(user: UserPrincipal) = UserResponseDto(
            id = user.id.toString(),
            externalId = user.externalId,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            photoUrl = user.photoUrl,
            lang = user.lang,
        )
    }
}