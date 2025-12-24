package com.ynixt.sharedfinances.domain.models.security

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt

class UserJwtAuthenticationToken(
    private val jwt: Jwt,
    private val user: UserPrincipal,
    authorities: Collection<GrantedAuthority>,
) : AbstractAuthenticationToken(authorities) {
    override fun getCredentials(): Jwt = jwt

    override fun getPrincipal(): UserPrincipal = user

    init {
        isAuthenticated = true
    }
}
