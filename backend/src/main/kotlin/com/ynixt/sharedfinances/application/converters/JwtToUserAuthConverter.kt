package com.ynixt.sharedfinances.application.converters

import com.ynixt.sharedfinances.domain.models.security.UserJwtAuthenticationToken
import com.ynixt.sharedfinances.domain.models.security.UserPrincipal
import com.ynixt.sharedfinances.domain.repositories.SessionRepository
import com.ynixt.sharedfinances.domain.repositories.UserRepository
import com.ynixt.sharedfinances.domain.services.ACCOUNT_ACTIVITY_WRITE_INTERVAL
import com.ynixt.sharedfinances.domain.services.SESSION_CLAIM_NAME
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Component
class JwtToUserAuthConverter(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val clock: Clock,
) : Converter<Jwt, Mono<AbstractAuthenticationToken>> {
    override fun convert(jwt: Jwt): Mono<AbstractAuthenticationToken> {
        val uid = jwt.subject ?: return Mono.error(BadCredentialsException("JWT without sub"))
        val session = jwt.claims[SESSION_CLAIM_NAME] as String? ?: return Mono.error(BadCredentialsException("JWT without session"))

        return sessionRepository
            .existsById(UUID.fromString(session))
            .flatMap { exits ->
                if (!exits) {
                    Mono.error(BadCredentialsException("Session not found"))
                } else {
                    userRepository
                        .findById(UUID.fromString(uid))
                }
            }.switchIfEmpty(Mono.error(BadCredentialsException("User not found")))
            .flatMap { user ->
                val usedAt = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)
                if (user.lastLoginAt.isBefore(usedAt.minus(ACCOUNT_ACTIVITY_WRITE_INTERVAL))) {
                    userRepository
                        .recordActivityIfOlderThan(
                            userId = user.id!!,
                            usedAt = usedAt,
                            cutoff = usedAt.minus(ACCOUNT_ACTIVITY_WRITE_INTERVAL),
                        ).thenReturn(
                            user.apply {
                                lastLoginAt = usedAt
                                inactivityNoticeStage = null
                            },
                        )
                } else {
                    Mono.just(user)
                }
            }.map { user ->
                val authorities =
                    listOf(
                        SimpleGrantedAuthority("ROLE_USER"),
                        SimpleGrantedAuthority("ROLE_${user.role.name}"),
                    ).distinct()
                val principal = UserPrincipal(user, authorities)

                UserJwtAuthenticationToken(jwt, principal, authorities).apply {
                    isAuthenticated = true
                }
            }
    }
}
