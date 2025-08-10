package com.ynixt.shared_finances.application.converters

import com.ynixt.shared_finances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.shared_finances.domain.models.security.UserPrincipal
import com.ynixt.shared_finances.domain.repositories.UserRepository
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class JwtToUserAuthConverter(
    private val userRepository: UserRepository,
) : Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    override fun convert(jwt: Jwt): Mono<AbstractAuthenticationToken> {
        val uid = jwt.subject ?: return Mono.error(BadCredentialsException("JWT without sub"))

        return userRepository.findByExternalId(uid)
            .next()
            .switchIfEmpty(Mono.error(BadCredentialsException("User not found")))
            .map { user ->
                val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
                val principal = UserPrincipal(user, authorities)

                UserJwtAuthenticationToken(jwt, principal, authorities).apply {
                    isAuthenticated = true
                }
            }
    }
}
