package com.ynixt.shared_finances.domain.models.security

import com.ynixt.shared_finances.domain.entities.User
import org.springframework.security.core.GrantedAuthority
import java.security.Principal
import java.util.*

class UserPrincipal(
    val id: UUID,
    val externalId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    var photoUrl: String? = null,
    var lang: String,
    val authorities: List<GrantedAuthority>,
) : Principal {
    constructor(user: User, authorities: List<GrantedAuthority>) : this(
        user.id!!,
        user.externalId,
        user.email,
        user.firstName,
        user.lastName,
        user.photoUrl,
        user.lang,
        authorities
    )

    override fun getName(): String {
        return firstName
    }
}