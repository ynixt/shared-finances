package com.ynixt.sharedfinances.domain.models.security

import com.ynixt.sharedfinances.domain.entities.UserEntity
import org.springframework.security.core.GrantedAuthority
import java.security.Principal
import java.util.UUID

class UserPrincipal(
    val id: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    var lang: String,
    var defaultCurrency: String,
    var tmz: String,
    var photoUrl: String?,
    var emailVerified: Boolean,
    var mfaEnabled: Boolean,
    var onboardingDone: Boolean,
    val authorities: List<GrantedAuthority>,
) : Principal {
    constructor(user: UserEntity, authorities: List<GrantedAuthority>) : this(
        user.id!!,
        user.email,
        user.firstName,
        user.lastName,
        user.lang,
        user.defaultCurrency,
        user.tmz,
        user.photoUrl,
        user.emailVerified,
        user.mfaEnabled,
        user.onboardingDone,
        authorities,
    )

    override fun getName(): String = firstName
}
