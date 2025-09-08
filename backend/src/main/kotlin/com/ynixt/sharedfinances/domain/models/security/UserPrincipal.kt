package com.ynixt.sharedfinances.domain.models.security

import com.ynixt.sharedfinances.domain.entities.User
import org.springframework.security.core.GrantedAuthority
import java.security.Principal
import java.util.UUID

class UserPrincipal(
    val id: UUID,
    val externalId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    var lang: String,
    var defaultCurrency: String?,
    val authorities: List<GrantedAuthority>,
) : Principal {
    constructor(user: User, authorities: List<GrantedAuthority>) : this(
        user.id!!,
        user.externalId,
        user.email,
        user.firstName,
        user.lastName,
        user.lang,
        user.defaultCurrency,
        authorities,
    )

    override fun getName(): String = firstName
}
